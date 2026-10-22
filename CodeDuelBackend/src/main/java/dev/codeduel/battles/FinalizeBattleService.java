package dev.codeduel.battles;

import dev.codeduel.common.DatabaseClock;
import dev.codeduel.common.Sql;
import dev.codeduel.events.BattleEvents;
import dev.codeduel.ratings.RatingService;
import dev.codeduel.submissions.Verdict;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalizeBattleService {
  private final JdbcClient jdbc;
  private final BattleStore battles;
  private final DatabaseClock clock;
  private final RatingService ratings;
  private final BattleEvents events;

  public FinalizeBattleService(
      JdbcClient jdbc,
      BattleStore battles,
      DatabaseClock clock,
      RatingService ratings,
      BattleEvents events) {
    this.jdbc = jdbc;
    this.battles = battles;
    this.clock = clock;
    this.ratings = ratings;
    this.events = events;
  }

  @Transactional
  public void attempt(UUID battleId) {
    var battle = battles.lock(battleId);
    if (battle.status().terminal()) return;
    var now = clock.now();
    if (battle.startsAt() == null) {
      if (!now.isBefore(battle.readyDeadline())) cancelLocked(battleId, "READY_TIMEOUT");
      return;
    }
    var official =
        jdbc.sql(
                "SELECT receive_sequence, user_id, verdict FROM submissions WHERE battle_id = :id AND is_official ORDER BY receive_sequence")
            .param("id", battleId)
            .query(
                (rs, row) ->
                    new OutcomeResolver.Candidate(
                        rs.getLong("receive_sequence"),
                        Sql.uuid(rs, "user_id"),
                        Verdict.valueOf(rs.getString("verdict"))))
            .list();
    var forfeiting =
        jdbc.sql(
                "SELECT user_id FROM battle_participants WHERE battle_id = :id AND forfeited_at IS NOT NULL ORDER BY forfeited_at, slot LIMIT 1")
            .param("id", battleId)
            .query(UUID.class)
            .optional()
            .orElse(null);
    UUID opponent =
        forfeiting == null
            ? null
            : jdbc.sql(
                    "SELECT user_id FROM battle_participants WHERE battle_id = :id AND user_id <> :user")
                .param("id", battleId)
                .param("user", forfeiting)
                .query(UUID.class)
                .single();
    var decision = OutcomeResolver.resolve(now, battle.endsAt(), official, forfeiting, opponent);
    if (decision.isPending()) {
      if (battle.status() == BattleStatus.IN_PROGRESS
          && (!now.isBefore(battle.endsAt())
              || official.stream().anyMatch(s -> s.verdict() == Verdict.ACCEPTED))) {
        jdbc.sql("UPDATE battles SET status = 'JUDGING' WHERE id = :id")
            .param("id", battleId)
            .update();
        events.append(battleId, "JUDGING_STARTED", Map.of());
      }
      return;
    }
    if (decision.status() == BattleStatus.FINISHED && battle.mode().equals("RANKED"))
      ratings.apply(battleId, decision.winnerId());
    jdbc.sql(
            "UPDATE battles SET status = :status, finished_at = clock_timestamp(), winner_id = :winner, finalization_reason = :reason, invite_hash = NULL WHERE id = :id")
        .param("status", decision.status().name())
        .param("winner", decision.winnerId(), java.sql.Types.OTHER)
        .param("reason", decision.reason())
        .param("id", battleId)
        .update();
    release(battleId);
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("status", decision.status());
    payload.put("winnerId", decision.winnerId());
    payload.put("reason", decision.reason());
    events.append(battleId, "BATTLE_FINISHED", payload);
  }

  @Transactional
  public void cancel(UUID battleId, String reason) {
    var battle = battles.lock(battleId);
    if (!battle.status().terminal() && battle.startsAt() == null) cancelLocked(battleId, reason);
  }

  private void cancelLocked(UUID battleId, String reason) {
    jdbc.sql(
            "UPDATE battles SET status = 'CANCELLED', finished_at = clock_timestamp(), finalization_reason = :reason, invite_hash = NULL WHERE id = :id")
        .param("reason", reason)
        .param("id", battleId)
        .update();
    release(battleId);
    events.append(battleId, "BATTLE_FINISHED", Map.of("status", "CANCELLED", "reason", reason));
  }

  private void release(UUID battleId) {
    jdbc.sql("DELETE FROM active_sessions WHERE battle_id = :id").param("id", battleId).update();
  }
}
