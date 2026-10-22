package dev.codeduel.matchmaking;

import dev.codeduel.battles.BattleService;
import dev.codeduel.battles.SessionGuard;
import dev.codeduel.common.ApiException;
import dev.codeduel.common.DatabaseClock;
import dev.codeduel.common.Sql;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchmakingService {
  private final JdbcClient jdbc;
  private final SessionGuard sessions;
  private final BattleService battles;
  private final DatabaseClock clock;
  private final int capacity;
  private final int staleSeconds;
  private final boolean requireVerified;

  public MatchmakingService(
      JdbcClient jdbc,
      SessionGuard sessions,
      BattleService battles,
      DatabaseClock clock,
      @Value("${codeduel.queue.capacity}") int capacity,
      @Value("${codeduel.queue.stale-seconds}") int staleSeconds,
      @Value("${codeduel.require-verified-email:false}") boolean requireVerified) {
    this.jdbc = jdbc;
    this.sessions = sessions;
    this.battles = battles;
    this.clock = clock;
    this.capacity = capacity;
    this.staleSeconds = staleSeconds;
    this.requireVerified = requireVerified;
  }

  @Transactional
  public State enter(UUID user) {
    sessions.lockUser(user);
    if (requireVerified
        && !jdbc.sql("SELECT email_verified FROM users WHERE id=:id")
            .param("id", user)
            .query(Boolean.class)
            .single())
      throw new ApiException(
          org.springframework.http.HttpStatus.FORBIDDEN,
          "EMAIL_VERIFICATION_REQUIRED",
          "Verify your email in account settings before entering ranked matchmaking.");
    int refreshed =
        jdbc.sql(
                "UPDATE matchmaking_entries SET heartbeat_at = clock_timestamp() WHERE user_id = :user")
            .param("user", user)
            .update();
    if (refreshed == 1) return state(user);
    int size = jdbc.sql("SELECT count(*) FROM matchmaking_entries").query(Integer.class).single();
    if (size >= capacity)
      throw ApiException.conflict(
          "QUEUE_FULL", "The queue is at capacity. Please try again shortly.");
    sessions.reserve(user, null);
    jdbc.sql(
            "INSERT INTO matchmaking_entries(user_id, rating_snapshot) SELECT user_id, rating FROM rating_profiles WHERE user_id = :user")
        .param("user", user)
        .update();
    return state(user);
  }

  @Transactional
  public State cancel(UUID user) {
    sessions.lockUser(user);
    jdbc.sql("DELETE FROM matchmaking_entries WHERE user_id = :user").param("user", user).update();
    jdbc.sql("DELETE FROM active_sessions WHERE user_id = :user AND battle_id IS NULL")
        .param("user", user)
        .update();
    return state(user);
  }

  public State state(UUID user) {
    var now = clock.now();
    return jdbc.sql(
            "SELECT a.battle_id, q.created_at FROM active_sessions a LEFT JOIN matchmaking_entries q ON q.user_id = a.user_id WHERE a.user_id = :user")
        .param("user", user)
        .query(
            (rs, row) -> {
              UUID battle = Sql.uuid(rs, "battle_id");
              Instant queuedAt = Sql.instant(rs, "created_at");
              return new State(
                  battle != null ? "MATCHED" : "QUEUED",
                  battle,
                  queuedAt,
                  now,
                  queuedAt == null ? 0 : RatingWindow.at(queuedAt, now));
            })
        .optional()
        .orElse(new State("IDLE", null, null, now, 0));
  }

  @Transactional
  public int pair() {
    // This single, transaction-scoped leader lock serializes pairing and stale-queue cleanup
    // across API instances. Row locks also serialize cancellation and heartbeat changes.
    boolean leader =
        jdbc.sql("SELECT pg_try_advisory_xact_lock(772406003)").query(Boolean.class).single();
    if (!leader) return 0;
    var stale =
        jdbc.sql(
                "SELECT user_id FROM matchmaking_entries WHERE heartbeat_at < clock_timestamp() - :seconds * interval '1 second' FOR UPDATE SKIP LOCKED")
            .param("seconds", staleSeconds)
            .query(UUID.class)
            .list();
    for (UUID user : stale) {
      jdbc.sql("DELETE FROM matchmaking_entries WHERE user_id = :user")
          .param("user", user)
          .update();
      jdbc.sql("DELETE FROM active_sessions WHERE user_id = :user AND battle_id IS NULL")
          .param("user", user)
          .update();
    }
    var entries =
        jdbc.sql(
                "SELECT * FROM matchmaking_entries ORDER BY created_at, user_id LIMIT 200 FOR UPDATE SKIP LOCKED")
            .query(
                (rs, row) ->
                    new Entry(
                        Sql.uuid(rs, "user_id"),
                        rs.getBigDecimal("rating_snapshot"),
                        Sql.instant(rs, "created_at")))
            .list();
    var now = clock.now();
    var claimed = new HashSet<UUID>();
    int count = 0;
    for (var first : entries) {
      if (claimed.contains(first.userId())) continue;
      Entry best = null;
      BigDecimal bestDifference = null;
      for (var second : entries) {
        if (first.userId().equals(second.userId()) || claimed.contains(second.userId())) continue;
        var difference = first.rating().subtract(second.rating()).abs();
        int window =
            Math.min(
                RatingWindow.at(first.createdAt(), now), RatingWindow.at(second.createdAt(), now));
        if (difference.compareTo(BigDecimal.valueOf(window)) > 0
            || (bestDifference != null && difference.compareTo(bestDifference) >= 0)) continue;
        boolean recent =
            jdbc.sql(
                    """
            SELECT EXISTS(SELECT 1 FROM battle_participants a JOIN battle_participants b ON a.battle_id = b.battle_id
              JOIN battles battle ON battle.id = a.battle_id
              WHERE a.user_id = :first AND b.user_id = :second AND battle.mode = 'RANKED'
                AND battle.status = 'FINISHED' AND battle.finished_at > clock_timestamp() - interval '10 minutes')
            """)
                .param("first", first.userId())
                .param("second", second.userId())
                .query(Boolean.class)
                .single();
        if (!recent) {
          best = second;
          bestDifference = difference;
        }
      }
      if (best != null) {
        battles.createRanked(first.userId(), best.userId());
        jdbc.sql("DELETE FROM matchmaking_entries WHERE user_id IN (:first, :second)")
            .param("first", first.userId())
            .param("second", best.userId())
            .update();
        claimed.add(first.userId());
        claimed.add(best.userId());
        count++;
      }
    }
    return count;
  }

  private record Entry(UUID userId, BigDecimal rating, Instant createdAt) {}

  public record State(
      String status, UUID battleId, Instant queuedAt, Instant serverTime, int ratingWindow) {}
}
