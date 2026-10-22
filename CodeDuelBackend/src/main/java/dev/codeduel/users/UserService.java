package dev.codeduel.users;

import dev.codeduel.common.ApiException;
import dev.codeduel.common.Sql;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final JdbcClient jdbc;
  private static final String PROFILE_SQL =
      """
      SELECT u.id, u.username, u.role, u.email_verified, r.rating, r.ranked_games, r.wins, r.losses, r.draws
      FROM users u JOIN rating_profiles r ON r.user_id = u.id
      """;
  private static final RowMapper<Profile> PROFILE_MAPPER =
      (rs, row) ->
          new Profile(
              Sql.uuid(rs, "id"),
              rs.getString("username"),
              rs.getBigDecimal("rating"),
              rs.getInt("ranked_games"),
              rs.getInt("wins"),
              rs.getInt("losses"),
              rs.getInt("draws"),
              rs.getInt("ranked_games") < 10,
              rs.getString("role"),
              rs.getBoolean("email_verified"));

  public UserService(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public Profile profile(UUID id) {
    return jdbc.sql(PROFILE_SQL + " WHERE u.id = :id AND u.deleted_at IS NULL")
        .param("id", id)
        .query(PROFILE_MAPPER)
        .optional()
        .orElseThrow(ApiException::notFound);
  }

  public Profile profile(String username) {
    return jdbc.sql(
            PROFILE_SQL + " WHERE lower(u.username) = lower(:username) AND u.deleted_at IS NULL")
        .param("username", username)
        .query(PROFILE_MAPPER)
        .optional()
        .orElseThrow(ApiException::notFound);
  }

  public List<Profile> leaderboard() {
    return jdbc.sql(
            PROFILE_SQL
                + " WHERE u.deleted_at IS NULL ORDER BY r.rating DESC, u.username ASC LIMIT 100")
        .query(PROFILE_MAPPER)
        .list();
  }

  public List<MatchHistory> history(UUID userId) {
    return jdbc.sql(
            """
        SELECT b.id, b.mode, b.status, b.winner_id, b.finished_at, b.created_at, p.title,
               opponent.username AS opponent, re.delta
        FROM battle_participants self
        JOIN battles b ON b.id = self.battle_id
        JOIN problem_versions pv ON pv.id = b.problem_version_id JOIN problems p ON p.id = pv.problem_id
        LEFT JOIN battle_participants other ON other.battle_id = b.id AND other.user_id <> :user
        LEFT JOIN users opponent ON opponent.id = other.user_id
        LEFT JOIN rating_events re ON re.battle_id = b.id AND re.user_id = :user
        WHERE self.user_id = :user ORDER BY b.created_at DESC LIMIT 30
        """)
        .param("user", userId)
        .query(
            (rs, row) ->
                new MatchHistory(
                    Sql.uuid(rs, "id"),
                    rs.getString("mode"),
                    rs.getString("status"),
                    Sql.uuid(rs, "winner_id"),
                    rs.getString("title"),
                    rs.getString("opponent"),
                    rs.getBigDecimal("delta"),
                    Sql.instant(rs, "created_at"),
                    Sql.instant(rs, "finished_at")))
        .list();
  }

  public List<RatingPoint> ratingHistory(String username) {
    UUID id = profile(username).id();
    return jdbc.sql(
            "SELECT battle_id,old_rating,new_rating,delta,created_at FROM rating_events WHERE user_id=:id ORDER BY created_at DESC LIMIT 100")
        .param("id", id)
        .query(
            (rs, row) ->
                new RatingPoint(
                    Sql.uuid(rs, "battle_id"),
                    rs.getBigDecimal("old_rating"),
                    rs.getBigDecimal("new_rating"),
                    rs.getBigDecimal("delta"),
                    Sql.instant(rs, "created_at")))
        .list()
        .reversed();
  }

  public record RatingPoint(
      UUID battleId,
      BigDecimal oldRating,
      BigDecimal newRating,
      BigDecimal delta,
      Instant occurredAt) {}

  public record Profile(
      UUID id,
      String username,
      BigDecimal rating,
      int rankedGames,
      int wins,
      int losses,
      int draws,
      boolean provisional,
      String role,
      boolean emailVerified) {}

  public record MatchHistory(
      UUID id,
      String mode,
      String status,
      UUID winnerId,
      String title,
      String opponent,
      BigDecimal delta,
      Instant createdAt,
      Instant finishedAt) {}
}
