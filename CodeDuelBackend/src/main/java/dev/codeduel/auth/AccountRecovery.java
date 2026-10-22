package dev.codeduel.auth;

import dev.codeduel.common.ApiException;
import dev.codeduel.common.SecretTokens;
import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountRecovery {
  private final JdbcClient jdbc;
  private final PasswordEncoder passwords;
  private final String publicUrl;

  public AccountRecovery(
      JdbcClient jdbc,
      PasswordEncoder passwords,
      @Value("${codeduel.public-url:http://localhost:5173}") String publicUrl) {
    URI uri = URI.create(publicUrl);
    if (uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null
        || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())))
      throw new IllegalArgumentException("Configure the application's canonical public URL");
    this.jdbc = jdbc;
    this.passwords = passwords;
    this.publicUrl = publicUrl.replaceAll("/+$", "");
  }

  @Transactional
  public void requestVerification(UUID user) {
    var email =
        jdbc.sql(
                "SELECT email FROM users WHERE id=:id AND deleted_at IS NULL AND NOT email_verified FOR NO KEY UPDATE")
            .param("id", user)
            .query(String.class)
            .optional();
    email.ifPresent(
        value ->
            issue(
                user, value, "VERIFY_EMAIL", 1440, "Verify your CodeDuel email", "/verify-email"));
  }

  @Transactional
  public void requestReset(String email) {
    jdbc.sql(
            "SELECT id FROM users WHERE lower(email)=lower(:email) AND deleted_at IS NULL FOR NO KEY UPDATE")
        .param("email", email)
        .query(UUID.class)
        .optional()
        .ifPresent(
            user ->
                issue(
                    user,
                    email,
                    "RESET_PASSWORD",
                    15,
                    "Reset your CodeDuel password",
                    "/reset-password"));
  }

  private void issue(
      UUID user, String email, String purpose, int minutes, String subject, String path) {
    jdbc.sql(
            "UPDATE auth_tokens SET consumed_at=clock_timestamp() WHERE user_id=:user AND purpose=:purpose AND consumed_at IS NULL")
        .param("user", user)
        .param("purpose", purpose)
        .update();
    String token = SecretTokens.create();
    jdbc.sql(
            "INSERT INTO auth_tokens(id,user_id,purpose,token_hash,expires_at) VALUES (:id,:user,:purpose,:hash,clock_timestamp()+:minutes*interval '1 minute')")
        .param("id", UUID.randomUUID())
        .param("user", user)
        .param("purpose", purpose)
        .param("hash", SecretTokens.hash(token))
        .param("minutes", minutes)
        .update();
    String body =
        "Use this one-time link:\n\n"
            + publicUrl
            + path
            + "#token="
            + token
            + "\n\nThis link expires in "
            + minutes
            + " minutes. If you did not request it, you can ignore this message.\n";
    jdbc.sql(
            "INSERT INTO mail_outbox(id,user_id,recipient,subject,body) VALUES (:id,:user,:email,:subject,:body)")
        .param("id", UUID.randomUUID())
        .param("user", user)
        .param("email", email)
        .param("subject", subject)
        .param("body", body)
        .update();
  }

  @Transactional
  public void verify(String token) {
    UUID user = consume(token, "VERIFY_EMAIL");
    jdbc.sql("UPDATE users SET email_verified=true WHERE id=:id").param("id", user).update();
  }

  @Transactional
  public void reset(String token, String password) {
    PasswordPolicy.validate(password);
    UUID user = consume(token, "RESET_PASSWORD");
    String username =
        jdbc.sql("UPDATE users SET password_hash=:password WHERE id=:id RETURNING username")
            .param("id", user)
            .param("password", passwords.encode(password))
            .query(String.class)
            .single();
    jdbc.sql("DELETE FROM spring_session WHERE principal_name=:name")
        .param("name", username)
        .update();
    jdbc.sql(
            "UPDATE auth_tokens SET consumed_at=clock_timestamp() WHERE user_id=:user AND consumed_at IS NULL AND purpose='RESET_PASSWORD'")
        .param("user", user)
        .update();
  }

  private UUID consume(String token, String purpose) {
    if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalid();
    String hash = SecretTokens.hash(token);
    UUID user =
        jdbc.sql("SELECT user_id FROM auth_tokens WHERE token_hash=:hash AND purpose=:purpose")
            .param("hash", hash)
            .param("purpose", purpose)
            .query(UUID.class)
            .optional()
            .orElseThrow(AccountRecovery::invalid);
    jdbc.sql("SELECT id FROM users WHERE id=:id AND deleted_at IS NULL FOR NO KEY UPDATE")
        .param("id", user)
        .query(UUID.class)
        .optional()
        .orElseThrow(AccountRecovery::invalid);
    UUID valid =
        jdbc.sql(
                "SELECT id FROM auth_tokens WHERE token_hash=:hash AND purpose=:purpose AND consumed_at IS NULL AND expires_at>clock_timestamp() FOR UPDATE")
            .param("hash", hash)
            .param("purpose", purpose)
            .query(UUID.class)
            .optional()
            .orElseThrow(AccountRecovery::invalid);
    jdbc.sql("UPDATE auth_tokens SET consumed_at=clock_timestamp() WHERE id=:id")
        .param("id", valid)
        .update();
    return user;
  }

  private static ApiException invalid() {
    return new ApiException(
        HttpStatus.BAD_REQUEST,
        "INVALID_LINK",
        "This link has expired or was already used. Request a new one.");
  }
}
