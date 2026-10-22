package dev.codeduel.submissions;

import dev.codeduel.battles.BattleStatus;
import dev.codeduel.battles.BattleStore;
import dev.codeduel.common.ApiException;
import dev.codeduel.common.DatabaseClock;
import dev.codeduel.common.Sql;
import dev.codeduel.events.BattleEvents;
import dev.codeduel.judge.JudgePolicy;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionService {
  private final JdbcClient jdbc;
  private final BattleStore battles;
  private final DatabaseClock clock;
  private final JudgePolicy policy;
  private final BattleEvents events;

  public SubmissionService(
      JdbcClient jdbc,
      BattleStore battles,
      DatabaseClock clock,
      JudgePolicy policy,
      BattleEvents events) {
    this.jdbc = jdbc;
    this.battles = battles;
    this.clock = clock;
    this.policy = policy;
    this.events = events;
  }

  @Transactional
  public Receipt submit(
      UUID battleId, UUID user, String language, String source, boolean official) {
    validateSource(language, source);
    lockQuota(user);
    // The battle is the first aggregate row lock for both receipt allocation and finalization.
    var battle = battles.lock(battleId);
    battles.requireMember(battleId, user);
    Instant receivedAt = clock.now();
    if ((battle.status() != BattleStatus.IN_PROGRESS && battle.status() != BattleStatus.JUDGING)
        || battle.startsAt() == null
        || receivedAt.isBefore(battle.startsAt())
        || !receivedAt.isBefore(battle.endsAt())) {
      throw ApiException.conflict(
          "SUBMISSIONS_CLOSED", "This battle is not accepting submissions.");
    }
    checkPolicy(battle.problemVersionId(), source, user);
    long sequence =
        jdbc.sql(
                "UPDATE battles SET receive_sequence = receive_sequence + 1 WHERE id = :id RETURNING receive_sequence")
            .param("id", battleId)
            .query(Long.class)
            .single();
    var receipt =
        persist(
            battleId,
            battle.problemVersionId(),
            user,
            source,
            official,
            official ? "BATTLE" : "PUBLIC_EXAMPLES",
            !official,
            receivedAt,
            sequence);
    if (official)
      events.append(
          battleId,
          "SUBMISSION_RECEIVED",
          Map.of("userId", user, "submissionId", receipt.submissionId()));
    return receipt;
  }

  @Transactional
  public Receipt practice(
      UUID version, UUID user, String language, String source, boolean examplesOnly) {
    validateSource(language, source);
    lockQuota(user);
    boolean published =
        jdbc.sql(
                "SELECT EXISTS(SELECT 1 FROM problem_versions pv JOIN problems p ON p.id = pv.problem_id WHERE pv.id = :id AND pv.status = 'PUBLISHED' AND p.active)")
            .param("id", version)
            .query(Boolean.class)
            .single();
    if (!published) throw ApiException.notFound();
    checkPolicy(version, source, user);
    return standalone(
        version, user, source, examplesOnly ? "PUBLIC_EXAMPLES" : "PRACTICE", examplesOnly);
  }

  /**
   * Administrator service freezes this version before enqueueing its immutable reference program.
   */
  @Transactional
  public Receipt verifyReference(UUID version, UUID administrator, String source) {
    validateSource("JAVA_21", source);
    if (!jdbc.sql(
            "SELECT EXISTS(SELECT 1 FROM problem_versions WHERE id = :id AND status = 'REVIEW')")
        .param("id", version)
        .query(Boolean.class)
        .single())
      throw ApiException.conflict(
          "VERSION_NOT_FROZEN", "Freeze the reference version before verification.");
    lockQuota(administrator);
    checkPolicy(version, source, administrator);
    return standalone(version, administrator, source, "VERIFICATION", false);
  }

  private Receipt standalone(
      UUID version, UUID user, String source, String purpose, boolean examplesOnly) {
    long sequence =
        jdbc.sql("SELECT nextval('practice_receive_sequence')").query(Long.class).single();
    return persist(
        null, version, user, source, false, purpose, examplesOnly, clock.now(), sequence);
  }

  private void lockQuota(UUID user) {
    jdbc.sql("SELECT id FROM users WHERE id=:id AND deleted_at IS NULL FOR NO KEY UPDATE")
        .param("id", user)
        .query(UUID.class)
        .optional()
        .orElseThrow(ApiException::notFound);
    jdbc.sql("SELECT pg_advisory_xact_lock(hashtextextended(:key, 0))")
        .param("key", "submission:" + user)
        .query()
        .singleRow();
  }

  private void checkPolicy(UUID version, String source, UUID user) {
    String slug =
        jdbc.sql(
                "SELECT p.slug FROM problems p JOIN problem_versions pv ON pv.problem_id = p.id WHERE pv.id = :id")
            .param("id", version)
            .query(String.class)
            .single();
    policy.check(source, slug);
    int pending =
        jdbc.sql("SELECT count(*) FROM submissions WHERE user_id = :user AND verdict = 'PENDING'")
            .param("user", user)
            .query(Integer.class)
            .single();
    if (pending >= 4)
      throw ApiException.conflict(
          "SUBMISSION_BACKLOG", "Wait for your pending programs to finish judging.");
    int backlog =
        jdbc.sql("SELECT count(*) FROM judge_jobs WHERE status IN ('PENDING','RUNNING')")
            .query(Integer.class)
            .single();
    if (backlog >= 250)
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "JUDGE_BUSY",
          "The judge queue is full. Please try again shortly.");
  }

  private static void validateSource(String language, String source) {
    if (!"JAVA_21".equals(language)
        || source == null
        || source.isBlank()
        || source.getBytes(StandardCharsets.UTF_8).length > 65536) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "INVALID_SOURCE",
          "Use Java 21 and a program no larger than 64 KiB.");
    }
  }

  private Receipt persist(
      UUID battle,
      UUID version,
      UUID user,
      String source,
      boolean official,
      String purpose,
      boolean examplesOnly,
      Instant receivedAt,
      long sequence) {
    UUID id = UUID.randomUUID();
    jdbc.sql(
            """
        INSERT INTO submissions(id, battle_id, user_id, problem_version_id, language, source_code, received_at, receive_sequence, is_official, purpose)
        VALUES (:id,:battle,:user,:version,'JAVA_21',:source,:received,:sequence,:official,:purpose)
        """)
        .param("id", id)
        .param("battle", battle, java.sql.Types.OTHER)
        .param("user", user)
        .param("version", version)
        .param("source", source)
        .param("received", java.sql.Timestamp.from(receivedAt))
        .param("sequence", sequence)
        .param("official", official)
        .param("purpose", purpose)
        .update();
    jdbc.sql("INSERT INTO judge_jobs(id,submission_id) VALUES (:id,:submission)")
        .param("id", UUID.randomUUID())
        .param("submission", id)
        .update();
    int count =
        jdbc.sql(
                """
        INSERT INTO submission_case_results(submission_id,test_case_id)
        SELECT :submission,id FROM test_cases WHERE problem_version_id = :version AND (NOT :examples OR is_public)
        """)
            .param("submission", id)
            .param("version", version)
            .param("examples", examplesOnly)
            .update();
    if (count == 0)
      throw new IllegalStateException("Published challenge has no eligible test cases");
    return new Receipt(id, Verdict.PENDING, receivedAt, sequence);
  }

  @Transactional(readOnly = true)
  public Detail own(UUID id, UUID user) {
    return jdbc.sql(
            "SELECT id,battle_id,source_code,language,verdict,received_at,judged_at,is_official,purpose FROM submissions WHERE id = :id AND user_id = :user")
        .param("id", id)
        .param("user", user)
        .query(
            (rs, row) ->
                new Detail(
                    Sql.uuid(rs, "id"),
                    Sql.uuid(rs, "battle_id"),
                    rs.getString("source_code"),
                    rs.getString("language"),
                    Verdict.valueOf(rs.getString("verdict")),
                    Sql.instant(rs, "received_at"),
                    Sql.instant(rs, "judged_at"),
                    rs.getBoolean("is_official"),
                    rs.getString("purpose"),
                    diagnostic(id),
                    publicResults(id)))
        .optional()
        .orElseThrow(ApiException::notFound);
  }

  public List<PracticeEntry> practiceHistory(UUID version, UUID user) {
    return jdbc.sql(
            "SELECT id, verdict, received_at, purpose FROM submissions WHERE user_id = :user AND problem_version_id = :version AND battle_id IS NULL AND purpose IN ('PRACTICE','PUBLIC_EXAMPLES') ORDER BY received_at DESC LIMIT 20")
        .param("user", user)
        .param("version", version)
        .query(
            (rs, row) ->
                new PracticeEntry(
                    Sql.uuid(rs, "id"),
                    Verdict.valueOf(rs.getString("verdict")),
                    Sql.instant(rs, "received_at"),
                    rs.getString("purpose")))
        .list();
  }

  private String diagnostic(UUID id) {
    return jdbc.sql(
            "SELECT r.compiler_diagnostic FROM submission_case_results r JOIN test_cases t ON t.id = r.test_case_id WHERE r.submission_id = :id AND r.compiler_diagnostic <> '' ORDER BY t.ordinal LIMIT 1")
        .param("id", id)
        .query(String.class)
        .optional()
        .orElse("");
  }

  private List<PublicResult> publicResults(UUID id) {
    return jdbc.sql(
            "SELECT t.ordinal,r.verdict,r.public_stdout,r.runtime_ms FROM submission_case_results r JOIN test_cases t ON t.id = r.test_case_id WHERE r.submission_id = :id AND t.is_public ORDER BY t.ordinal")
        .param("id", id)
        .query(
            (rs, row) ->
                new PublicResult(
                    rs.getInt("ordinal"),
                    rs.getString("verdict"),
                    rs.getString("public_stdout"),
                    (Integer) rs.getObject("runtime_ms")))
        .list();
  }

  public record Receipt(UUID submissionId, Verdict verdict, Instant receivedAt, long sequence) {}

  public record PublicResult(int ordinal, String verdict, String stdout, Integer runtimeMs) {}

  public record PracticeEntry(UUID id, Verdict verdict, Instant receivedAt, String purpose) {}

  public record Detail(
      UUID id,
      UUID battleId,
      String sourceCode,
      String language,
      Verdict verdict,
      Instant receivedAt,
      Instant judgedAt,
      boolean official,
      String purpose,
      String compilerDiagnostic,
      List<PublicResult> publicResults) {}
}
