package dev.codeduel.auth;

import dev.codeduel.battles.SessionGuard;
import dev.codeduel.common.ApiException;
import dev.codeduel.common.AuditLog;
import dev.codeduel.matchmaking.MatchmakingService;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDataService {
  private static final String TOMBSTONE = "// Source removed by retention policy.";
  private final JdbcClient jdbc;
  private final PasswordEncoder passwords;
  private final SessionGuard sessions;
  private final MatchmakingService matchmaking;
  private final AuditLog audit;
  private final int retentionDays;

  public AccountDataService(
      JdbcClient jdbc,
      PasswordEncoder passwords,
      SessionGuard sessions,
      MatchmakingService matchmaking,
      AuditLog audit,
      @Value("${codeduel.source-retention-days:90}") int retentionDays) {
    if (retentionDays < 1 || retentionDays > 3650)
      throw new IllegalArgumentException(
          "Choose a source retention policy between 1 and 3650 days");
    this.jdbc = jdbc;
    this.passwords = passwords;
    this.sessions = sessions;
    this.matchmaking = matchmaking;
    this.audit = audit;
    this.retentionDays = retentionDays;
  }

  @Transactional
  public void delete(UUID user, String password) {
    sessions.lockUser(user);
    String hash =
        jdbc.sql("SELECT password_hash FROM users WHERE id=:id")
            .param("id", user)
            .query(String.class)
            .single();
    if (!passwords.matches(password, hash))
      throw new ApiException(
          HttpStatus.UNAUTHORIZED,
          "INVALID_PASSWORD",
          "Your current password is required to delete this account.");
    matchmaking.cancel(user);
    if (jdbc.sql("SELECT EXISTS(SELECT 1 FROM active_sessions WHERE user_id=:id)")
        .param("id", user)
        .query(Boolean.class)
        .single())
      throw ApiException.conflict(
          "ACTIVE_BATTLE", "Finish or forfeit your current battle before deleting this account.");
    String username =
        jdbc.sql("SELECT username FROM users WHERE id=:id")
            .param("id", user)
            .query(String.class)
            .single();
    jdbc.sql(
            "UPDATE judge_jobs j SET status='CANCELLED',lease_token=NULL,lease_until=NULL,last_error='ACCOUNT_DELETED' FROM submissions s WHERE j.submission_id=s.id AND s.user_id=:user AND j.status IN ('PENDING','RUNNING')")
        .param("user", user)
        .update();
    jdbc.sql(
            "UPDATE submissions SET verdict='JUDGE_ERROR',judged_at=clock_timestamp() WHERE user_id=:user AND verdict='PENDING'")
        .param("user", user)
        .update();
    jdbc.sql(
            "UPDATE submissions SET source_code=:tombstone,source_deleted_at=clock_timestamp() WHERE user_id=:user AND source_deleted_at IS NULL")
        .param("user", user)
        .param("tombstone", TOMBSTONE)
        .update();
    jdbc.sql(
            "UPDATE submission_case_results r SET compiler_diagnostic='',public_stdout='',executor_token=NULL FROM submissions s WHERE s.id=r.submission_id AND s.user_id=:user")
        .param("user", user)
        .update();
    jdbc.sql("DELETE FROM auth_tokens WHERE user_id=:id").param("id", user).update();
    jdbc.sql("DELETE FROM mail_outbox WHERE user_id=:id").param("id", user).update();
    jdbc.sql("DELETE FROM spring_session WHERE principal_name=:name")
        .param("name", username)
        .update();
    jdbc.sql(
            "UPDATE users SET username=:name,email=:email,password_hash='!deleted',role='USER',email_verified=false,deleted_at=clock_timestamp() WHERE id=:id")
        .param("name", "deleted_" + user.toString().replace("-", "").substring(0, 16))
        .param("email", user + "@deleted.invalid")
        .param("id", user)
        .update();
    audit.append(user, "ACCOUNT_ANONYMIZED", "USER", user);
  }

  @Transactional
  public int purgeExpiredSources() {
    int count =
        jdbc.sql(
                """
        WITH expired AS (SELECT id FROM submissions WHERE source_deleted_at IS NULL AND verdict<>'PENDING'
          AND received_at<clock_timestamp()-:days*interval '1 day' ORDER BY received_at FOR UPDATE SKIP LOCKED LIMIT 100)
        UPDATE submissions s SET source_code=:tombstone,source_deleted_at=clock_timestamp()
        FROM expired WHERE s.id=expired.id
        """)
            .param("days", retentionDays)
            .param("tombstone", TOMBSTONE)
            .update();
    jdbc.sql(
            "UPDATE submission_case_results r SET compiler_diagnostic='',public_stdout='',executor_token=NULL FROM submissions s WHERE r.submission_id=s.id AND s.source_deleted_at IS NOT NULL AND (r.compiler_diagnostic<>'' OR r.public_stdout<>'' OR r.executor_token IS NOT NULL)")
        .update();
    jdbc.sql("DELETE FROM auth_tokens WHERE expires_at<clock_timestamp()-interval '1 day'")
        .update();
    jdbc.sql("DELETE FROM mail_outbox WHERE created_at<clock_timestamp()-interval '1 day'")
        .update();
    return count;
  }

  public Account account(UUID user) {
    return jdbc.sql("SELECT email,email_verified FROM users WHERE id=:id AND deleted_at IS NULL")
        .param("id", user)
        .query(
            (rs, row) ->
                new Account(
                    rs.getString("email"),
                    rs.getBoolean("email_verified"),
                    "JAVA_21",
                    retentionDays))
        .optional()
        .orElseThrow(ApiException::notFound);
  }

  public record Account(
      String email, boolean emailVerified, String language, int sourceRetentionDays) {}
}
