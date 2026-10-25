package dev.codeduel.battles;

import dev.codeduel.common.ApiException;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionGuard {
  private final JdbcClient jdbc;

  public SessionGuard(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void lockUser(UUID user) {
    jdbc.sql("SELECT id FROM users WHERE id = :id AND deleted_at IS NULL FOR NO KEY UPDATE")
        .param("id", user)
        .query(UUID.class)
        .optional()
        .orElseThrow(ApiException::notFound);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void reserve(UUID user, UUID battle) {
    int inserted =
        jdbc.sql(
                "INSERT INTO active_sessions(user_id, battle_id) VALUES (:user, :battle) ON CONFLICT DO NOTHING")
            .param("user", user)
            .param("battle", battle, java.sql.Types.OTHER)
            .update();
    if (inserted != 1)
      throw ApiException.conflict(
          "ACTIVE_SESSION", "Finish or leave your current queue or battle first.");
  }
}
