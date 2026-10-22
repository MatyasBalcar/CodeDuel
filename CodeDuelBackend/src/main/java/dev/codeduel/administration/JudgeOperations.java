package dev.codeduel.administration;

import dev.codeduel.common.ApiException;
import dev.codeduel.common.AuditLog;
import dev.codeduel.common.Sql;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JudgeOperations {
  private final JdbcClient jdbc;
  private final AuditLog audit;

  public JudgeOperations(JdbcClient jdbc, AuditLog audit) {
    this.jdbc = jdbc;
    this.audit = audit;
  }

  public List<Job> jobs() {
    return jdbc.sql(
            "SELECT id, submission_id, status, failures, last_error, created_at, lease_until, (lease_until <= clock_timestamp()) AS lease_expired FROM judge_jobs WHERE status NOT IN ('COMPLETED','CANCELLED') ORDER BY created_at LIMIT 100")
        .query(
            (rs, row) ->
                new Job(
                    Sql.uuid(rs, "id"),
                    Sql.uuid(rs, "submission_id"),
                    rs.getString("status"),
                    rs.getInt("failures"),
                    rs.getString("last_error"),
                    Sql.instant(rs, "created_at"),
                    Sql.instant(rs, "lease_until"),
                    rs.getBoolean("lease_expired")))
        .list();
  }

  @Transactional
  public void releaseExpired(UUID id, UUID actor) {
    int updated =
        jdbc.sql(
                """
        UPDATE judge_jobs SET status = 'PENDING', lease_token = NULL, lease_until = NULL,
          executor_program = NULL, next_retry_at = clock_timestamp()
        WHERE id = :id AND status = 'RUNNING' AND lease_until <= clock_timestamp()
        """)
            .param("id", id)
            .update();
    if (updated != 1)
      throw ApiException.conflict(
          "JOB_NOT_EXPIRED",
          "Only expired, nonterminal leases can be released. Terminal verdicts remain immutable.");
    jdbc.sql(
            "UPDATE submission_case_results SET executor_token = NULL WHERE submission_id = (SELECT submission_id FROM judge_jobs WHERE id = :id) AND verdict IS NULL")
        .param("id", id)
        .update();
    audit.append(actor, "JUDGE_LEASE_RELEASED", "JUDGE_JOB", id);
  }

  public record Job(
      UUID id,
      UUID submissionId,
      String status,
      int failures,
      String lastError,
      Instant createdAt,
      Instant leaseUntil,
      boolean leaseExpired) {}
}
