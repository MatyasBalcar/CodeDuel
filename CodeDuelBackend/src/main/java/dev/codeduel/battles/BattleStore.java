package dev.codeduel.battles;

import dev.codeduel.common.ApiException;
import dev.codeduel.common.Sql;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class BattleStore {
  private final JdbcClient jdbc;

  public BattleStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public Battle lock(UUID id) {
    return jdbc.sql("SELECT * FROM battles WHERE id = :id FOR UPDATE")
        .param("id", id)
        .query(
            (rs, row) ->
                new Battle(
                    id,
                    Sql.uuid(rs, "problem_version_id"),
                    rs.getString("mode"),
                    BattleStatus.valueOf(rs.getString("status")),
                    Sql.instant(rs, "ready_deadline"),
                    Sql.instant(rs, "starts_at"),
                    Sql.instant(rs, "ends_at"),
                    Sql.uuid(rs, "winner_id"),
                    rs.getString("finalization_reason")))
        .optional()
        .orElseThrow(ApiException::notFound);
  }

  /** A missing resource and another player's resource intentionally have the same response. */
  public void requireMember(UUID battle, UUID user) {
    boolean member =
        jdbc.sql(
                "SELECT EXISTS(SELECT 1 FROM battle_participants WHERE battle_id = :battle AND user_id = :user)")
            .param("battle", battle)
            .param("user", user)
            .query(Boolean.class)
            .single();
    if (!member) throw ApiException.notFound();
  }

  public record Battle(
      UUID id,
      UUID problemVersionId,
      String mode,
      BattleStatus status,
      Instant readyDeadline,
      Instant startsAt,
      Instant endsAt,
      UUID winnerId,
      String finalizationReason) {}
}
