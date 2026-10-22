package dev.codeduel.battles;

import dev.codeduel.common.ApiException;
import dev.codeduel.common.DatabaseClock;
import dev.codeduel.common.Sql;
import dev.codeduel.problems.ProblemService;
import dev.codeduel.submissions.Verdict;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BattleQueries {
  private final JdbcClient jdbc;
  private final BattleStore battles;
  private final DatabaseClock clock;
  private final ProblemService problems;

  public BattleQueries(
      JdbcClient jdbc, BattleStore battles, DatabaseClock clock, ProblemService problems) {
    this.jdbc = jdbc;
    this.battles = battles;
    this.clock = clock;
    this.problems = problems;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public Snapshot snapshot(UUID id, UUID user) {
    battles.requireMember(id, user);
    Instant now = clock.now();
    var row = jdbc.sql("SELECT * FROM battles WHERE id = :id").param("id", id).query().singleRow();
    Instant startsAt = timestamp(row.get("starts_at"));
    Instant endsAt = timestamp(row.get("ends_at"));
    var players =
        jdbc.sql(
                """
        SELECT bp.*, u.username, r.rating, r.ranked_games, re.old_rating, re.delta, re.new_rating
        FROM battle_participants bp JOIN users u ON u.id = bp.user_id JOIN rating_profiles r ON r.user_id = u.id
        LEFT JOIN rating_events re ON re.battle_id = bp.battle_id AND re.user_id = bp.user_id
        WHERE bp.battle_id = :id ORDER BY bp.slot
        """)
            .param("id", id)
            .query(
                (rs, n) ->
                    new Player(
                        Sql.uuid(rs, "user_id"),
                        rs.getString("username"),
                        rs.getInt("slot"),
                        rs.getBigDecimal("rating"),
                        rs.getInt("ranked_games") < 10,
                        rs.getBoolean("is_ready"),
                        Sql.instant(rs, "forfeited_at"),
                        rs.getBigDecimal("old_rating"),
                        rs.getBigDecimal("delta"),
                        rs.getBigDecimal("new_rating")))
            .list();
    var own =
        jdbc.sql(
                "SELECT id, verdict, received_at, receive_sequence, is_official FROM submissions WHERE battle_id = :id AND user_id = :user ORDER BY receive_sequence DESC LIMIT 50")
            .param("id", id)
            .param("user", user)
            .query(
                (rs, n) ->
                    new OwnSubmission(
                        Sql.uuid(rs, "id"),
                        Verdict.valueOf(rs.getString("verdict")),
                        Sql.instant(rs, "received_at"),
                        rs.getLong("receive_sequence"),
                        rs.getBoolean("is_official")))
            .list();
    var problem =
        startsAt != null && !now.isBefore(startsAt)
            ? problems.version((UUID) row.get("problem_version_id"))
            : null;
    return new Snapshot(
        id,
        (String) row.get("mode"),
        BattleStatus.valueOf((String) row.get("status")),
        now,
        timestamp(row.get("ready_deadline")),
        startsAt,
        endsAt,
        timestamp(row.get("finished_at")),
        (UUID) row.get("winner_id"),
        (String) row.get("finalization_reason"),
        ((Number) row.get("event_sequence")).longValue(),
        players,
        problem,
        own);
  }

  private static Instant timestamp(Object value) {
    if (value == null) return null;
    if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
    throw new IllegalStateException("Unexpected database timestamp type");
  }

  public void requireFinished(UUID id, UUID user) {
    battles.requireMember(id, user);
    String status =
        jdbc.sql("SELECT status FROM battles WHERE id = :id")
            .param("id", id)
            .query(String.class)
            .single();
    if (!BattleStatus.valueOf(status).terminal())
      throw ApiException.conflict("MATCH_ACTIVE", "The replay is available after the battle ends.");
  }

  public record Snapshot(
      UUID id,
      String mode,
      BattleStatus status,
      Instant serverTime,
      Instant readyDeadline,
      Instant startsAt,
      Instant endsAt,
      Instant finishedAt,
      UUID winnerId,
      String finalizationReason,
      long sequence,
      List<Player> players,
      ProblemService.Detail problem,
      List<OwnSubmission> submissions) {}

  public record Player(
      UUID userId,
      String username,
      int slot,
      BigDecimal rating,
      boolean provisional,
      boolean ready,
      Instant forfeitedAt,
      BigDecimal oldRating,
      BigDecimal delta,
      BigDecimal newRating) {}

  public record OwnSubmission(
      UUID id, Verdict verdict, Instant receivedAt, long sequence, boolean official) {}
}
