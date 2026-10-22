package dev.codeduel.auth;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  private final JdbcClient jdbc;
  private final PasswordEncoder passwords;
  private final AccountRecovery recovery;

  public AccountService(JdbcClient jdbc, PasswordEncoder passwords, AccountRecovery recovery) {
    this.jdbc = jdbc;
    this.passwords = passwords;
    this.recovery = recovery;
  }

  @Transactional
  public UUID register(String username, String email, String password) {
    PasswordPolicy.validate(password);
    UUID id = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO users(id, username, email, password_hash) VALUES (:id, :username, :email, :password)")
        .param("id", id)
        .param("username", username)
        .param("email", email.toLowerCase(java.util.Locale.ROOT))
        .param("password", passwords.encode(password))
        .update();
    jdbc.sql("INSERT INTO rating_profiles(user_id) VALUES (:id)").param("id", id).update();
    recovery.requestVerification(id);
    return id;
  }
}
