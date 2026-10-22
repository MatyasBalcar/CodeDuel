package dev.codeduel.judge;

import dev.codeduel.battles.BattleStore;
import dev.codeduel.battles.FinalizeBattleService;
import dev.codeduel.common.Sql;
import dev.codeduel.events.BattleEvents;
import dev.codeduel.submissions.Verdict;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JudgeJobStore {
  private final JdbcClient jdbc;
  private final BattleStore battles;
  private final BattleEvents events;
  private final FinalizeBattleService finalizer;

  public JudgeJobStore(
      JdbcClient jdbc, BattleStore battles, BattleEvents events, FinalizeBattleService finalizer) {
    this.jdbc = jdbc;
    this.battles = battles;
    this.events = events;
    this.finalizer = finalizer;
  }

  /** Claim and commit before any network call. Expired leases are reclaimable after a crash. */
  @Transactional
  public Optional<Job> claim() {
    UUID lease = UUID.randomUUID();
    var claimed =
        jdbc.sql(
                """
        WITH next_job AS (
          SELECT id FROM judge_jobs
          WHERE (status = 'PENDING' AND next_retry_at <= clock_timestamp())
             OR (status = 'RUNNING' AND lease_until <= clock_timestamp())
          ORDER BY created_at, id FOR UPDATE SKIP LOCKED LIMIT 1
        )
        UPDATE judge_jobs j SET status = 'RUNNING', lease_token = :lease,
          lease_until = clock_timestamp() + interval '60 seconds'
        FROM next_job WHERE j.id = next_job.id RETURNING j.id, j.submission_id, j.created_at
        """)
            .param("lease", lease)
            .query(
                (rs, row) ->
                    new Claim(
                        Sql.uuid(rs, "id"),
                        Sql.uuid(rs, "submission_id"),
                        Sql.instant(rs, "created_at")))
            .optional();
    return claimed.map(
        claim ->
            jdbc.sql(
                    """
        SELECT s.battle_id, s.source_code, s.is_official, pv.time_limit_ms, pv.memory_limit_mb, j.executor_program
        FROM submissions s JOIN problem_versions pv ON pv.id = s.problem_version_id
        JOIN judge_jobs j ON j.submission_id = s.id WHERE s.id = :id
        """)
                .param("id", claim.submissionId())
                .query(
                    (rs, row) ->
                        new Job(
                            claim.id(),
                            claim.submissionId(),
                            Sql.uuid(rs, "battle_id"),
                            lease,
                            rs.getString("source_code"),
                            rs.getBoolean("is_official"),
                            rs.getInt("time_limit_ms"),
                            rs.getInt("memory_limit_mb"),
                            rs.getString("executor_program"),
                            claim.createdAt()))
                .single());
  }

  public List<Case> remaining(Job job) {
    return jdbc.sql(
            """
        SELECT r.test_case_id, COALESCE(r.executor_token, j.executor_program) AS executor_token, t.input_data, t.expected_output
        FROM submission_case_results r JOIN test_cases t ON t.id = r.test_case_id
        JOIN judge_jobs j ON j.submission_id = r.submission_id
        WHERE r.submission_id = :id AND r.verdict IS NULL ORDER BY t.ordinal LIMIT 1
        """)
        .param("id", job.submissionId())
        .query(
            (rs, row) ->
                new Case(
                    Sql.uuid(rs, "test_case_id"),
                    rs.getString("executor_token"),
                    rs.getString("input_data"),
                    rs.getString("expected_output")))
        .list();
  }

  @Transactional
  public boolean saveProgram(Job job, String token) {
    if (!ownsLease(job)) return false;
    jdbc.sql(
            "UPDATE judge_jobs SET executor_program = :token WHERE id = :id AND executor_program IS NULL")
        .param("token", token)
        .param("id", job.id())
        .update();
    return true;
  }

  @Transactional
  public boolean saveToken(Job job, UUID testCase, String token) {
    if (!ownsLease(job)) return false;
    jdbc.sql(
            "UPDATE submission_case_results SET executor_token = :token WHERE submission_id = :submission AND test_case_id = :test AND executor_token IS NULL")
        .param("token", token)
        .param("submission", job.submissionId())
        .param("test", testCase)
        .update();
    return true;
  }

  @Transactional
  public boolean saveResult(Job job, UUID testCase, JudgeGateway.Result result) {
    if (!result.verdict().terminal() || result.verdict() == Verdict.JUDGE_ERROR)
      throw new IllegalArgumentException("Only player verdicts may be stored as case results");
    if (!ownsLease(job)) return false;
    jdbc.sql(
            """
            UPDATE submission_case_results r SET verdict = :verdict, runtime_ms = :time,
              memory_kb = :memory, compiler_diagnostic = :diagnostic,
              public_stdout = CASE WHEN t.is_public THEN :stdout ELSE '' END
            FROM test_cases t WHERE r.test_case_id = t.id AND r.submission_id = :submission
              AND r.test_case_id = :test AND r.verdict IS NULL
            """)
        .param("verdict", result.verdict().name())
        .param("time", result.runtimeMs())
        .param("memory", result.memoryKb())
        .param(
            "diagnostic",
            result.verdict() == Verdict.COMPILATION_ERROR
                ? JudgeFeedback.safe(result.diagnostic())
                : "")
        .param("stdout", JudgeFeedback.safe(result.stdout()))
        .param("submission", job.submissionId())
        .param("test", testCase)
        .update();
    if (result.verdict() == Verdict.COMPILATION_ERROR) {
      jdbc.sql(
              """
          UPDATE submission_case_results SET verdict = 'COMPILATION_ERROR', compiler_diagnostic = :diagnostic
          WHERE submission_id = :id AND verdict IS NULL
          """)
          .param("id", job.submissionId())
          .param("diagnostic", JudgeFeedback.safe(result.diagnostic()))
          .update();
    }
    return true;
  }

  @Transactional
  public boolean finishOrReschedule(Job job) {
    return finishOrReschedule(job, "UNSPECIFIED");
  }

  @Transactional
  public boolean finishOrReschedule(Job job, String executorKind) {
    if (job.battleId() != null) battles.lock(job.battleId());
    if (!ownsLease(job)) return false;
    int remaining =
        jdbc.sql(
                "SELECT count(*) FROM submission_case_results WHERE submission_id = :id AND verdict IS NULL")
            .param("id", job.submissionId())
            .query(Integer.class)
            .single();
    if (remaining > 0) {
      jdbc.sql(
              "UPDATE judge_jobs SET status = 'PENDING', lease_token = NULL, lease_until = NULL, next_retry_at = clock_timestamp() + interval '500 milliseconds' WHERE id = :id")
          .param("id", job.id())
          .update();
      return false;
    }
    // Fixed testcase order makes aggregation independent of executor completion order.
    Verdict verdict =
        jdbc.sql(
                """
        SELECT r.verdict FROM submission_case_results r JOIN test_cases t ON t.id = r.test_case_id
        WHERE r.submission_id = :id AND r.verdict <> 'ACCEPTED' ORDER BY t.ordinal LIMIT 1
        """)
            .param("id", job.submissionId())
            .query(String.class)
            .optional()
            .map(Verdict::valueOf)
            .orElse(Verdict.ACCEPTED);
    jdbc.sql(
            "UPDATE judge_jobs SET status = 'COMPLETED', executor_kind = :kind, lease_token = NULL, lease_until = NULL, last_error = NULL WHERE id = :id")
        .param("kind", executorKind)
        .param("id", job.id())
        .update();
    commitVerdict(job, verdict);
    return true;
  }

  @Transactional
  public void fail(Job job) {
    if (job.battleId() != null) battles.lock(job.battleId());
    if (!ownsLease(job)) return;
    int failures =
        jdbc.sql(
                "UPDATE judge_jobs SET failures = failures + 1, last_error = 'EXECUTOR_UNAVAILABLE' WHERE id = :id RETURNING failures")
            .param("id", job.id())
            .query(Integer.class)
            .single();
    if (failures >= 3 || job.createdAt().isBefore(Instant.now().minusSeconds(300))) {
      jdbc.sql(
              "UPDATE judge_jobs SET status = 'DEAD', lease_token = NULL, lease_until = NULL WHERE id = :id")
          .param("id", job.id())
          .update();
      commitVerdict(job, Verdict.JUDGE_ERROR);
    } else {
      // Executor caches may have restarted or expired. Repeating uncommitted work is safe.
      jdbc.sql(
              "UPDATE submission_case_results SET executor_token = NULL WHERE submission_id = :id AND verdict IS NULL")
          .param("id", job.submissionId())
          .update();
      jdbc.sql(
              "UPDATE judge_jobs SET status = 'PENDING', executor_program = NULL, lease_token = NULL, lease_until = NULL, next_retry_at = clock_timestamp() + :delay * interval '1 second' WHERE id = :id")
          .param("delay", 1 << failures)
          .param("id", job.id())
          .update();
    }
  }

  /**
   * External jobs stuck in a nonterminal executor state eventually become infrastructure failures.
   */
  public boolean expired(Job job) {
    return job.createdAt().isBefore(Instant.now().minusSeconds(300));
  }

  private boolean ownsLease(Job job) {
    // Fencing token AND expiry are checked under the row lock on every write.
    return jdbc.sql(
            "SELECT status = 'RUNNING' AND lease_token = :lease AND lease_until > clock_timestamp() FROM judge_jobs WHERE id = :id FOR UPDATE")
        .param("lease", job.leaseToken())
        .param("id", job.id())
        .query(Boolean.class)
        .single();
  }

  private void commitVerdict(Job job, Verdict verdict) {
    int updated =
        jdbc.sql(
                "UPDATE submissions SET verdict = :verdict, judged_at = clock_timestamp() WHERE id = :id AND verdict = 'PENDING'")
            .param("verdict", verdict.name())
            .param("id", job.submissionId())
            .update();
    if (updated == 1 && job.official()) {
      events.append(
          job.battleId(),
          "SUBMISSION_JUDGED",
          Map.of(
              "userId",
              author(job.submissionId()),
              "submissionId",
              job.submissionId(),
              "status",
              verdict == Verdict.ACCEPTED
                  ? "ACCEPTED"
                  : verdict == Verdict.JUDGE_ERROR ? "JUDGE_UNAVAILABLE" : "ATTEMPTED"));
      finalizer.attempt(job.battleId());
    }
  }

  private UUID author(UUID submission) {
    return jdbc.sql("SELECT user_id FROM submissions WHERE id = :id")
        .param("id", submission)
        .query(UUID.class)
        .single();
  }

  private record Claim(UUID id, UUID submissionId, Instant createdAt) {}

  public record Job(
      UUID id,
      UUID submissionId,
      UUID battleId,
      UUID leaseToken,
      String source,
      boolean official,
      int timeLimitMs,
      int memoryLimitMb,
      String programToken,
      Instant createdAt) {}

  public record Case(UUID id, String executorToken, String input, String expected) {}
}
