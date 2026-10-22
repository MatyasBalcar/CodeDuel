package dev.codeduel.administration;

import dev.codeduel.auth.AccountService;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
@ConditionalOnProperty(name = "codeduel.bootstrap.username")
public class BootstrapAdministrator implements ApplicationRunner {
  private final AccountService accounts;
  private final JdbcClient jdbc;
  private final String username, email, password;

  public BootstrapAdministrator(
      AccountService accounts,
      JdbcClient jdbc,
      @Value("${codeduel.bootstrap.username}") String username,
      @Value("${codeduel.bootstrap.email}") String email,
      @Value("${codeduel.bootstrap.password}") String password) {
    this.accounts = accounts;
    this.jdbc = jdbc;
    this.username = username;
    this.email = email;
    this.password = password;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!username.matches("[A-Za-z][A-Za-z0-9_]{2,23}")
        || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
        || password.length() < 24
        || password.length() > 72)
      throw new IllegalStateException(
          "Configure a valid bootstrap administrator with a 24–72 character password");
    jdbc.sql("SELECT pg_advisory_xact_lock(772406004)").query().singleRow();
    var role =
        jdbc.sql("SELECT role FROM users WHERE lower(username)=lower(:name)")
            .param("name", username)
            .query(String.class)
            .optional();
    if (role.isPresent()) {
      if (!role.get().equals("ADMIN"))
        throw new IllegalStateException("Bootstrap username already belongs to a regular account");
      return;
    }
    UUID id = accounts.register(username, email, password);
    jdbc.sql("DELETE FROM auth_tokens WHERE user_id=:id").param("id", id).update();
    jdbc.sql("DELETE FROM mail_outbox WHERE user_id=:id").param("id", id).update();
    jdbc.sql("UPDATE users SET role='ADMIN',email_verified=true WHERE id=:id")
        .param("id", id)
        .update();
  }
}
