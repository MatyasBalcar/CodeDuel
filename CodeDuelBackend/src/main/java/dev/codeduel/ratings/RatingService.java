package dev.codeduel.ratings;

import dev.codeduel.common.Sql;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {
  private final JdbcClient jdbc;

  public RatingService(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  /** Caller holds the battle lock. Both changes use the same pre-match snapshot. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void apply(UUID battleId, UUID winnerId) {
    var profiles =
        jdbc.sql(
                """
        SELECT r.* FROM rating_profiles r JOIN battle_participants bp ON bp.user_id = r.user_id
        WHERE bp.battle_id = :battle ORDER BY r.user_id FOR UPDATE OF r
        """)
            .param("battle", battleId)
            .query(
                (rs, row) ->
                    new Before(
                        Sql.uuid(rs, "user_id"),
                        rs.getBigDecimal("rating"),
                        rs.getInt("ranked_games")))
            .list();
    if (profiles.size() != 2)
      throw new IllegalStateException("A ranked battle requires two profiles");
    for (int i = 0; i < 2; i++) {
      var player = profiles.get(i);
      var opponent = profiles.get(1 - i);
      double score = winnerId == null ? 0.5 : winnerId.equals(player.userId()) ? 1 : 0;
      BigDecimal delta =
          EloCalculator.delta(player.rating(), opponent.rating(), score, player.games());
      jdbc.sql(
              """
          INSERT INTO rating_events(id, battle_id, user_id, old_rating, delta, new_rating)
          VALUES (:id, :battle, :user, :old, :delta, :new)
          """)
          .param("id", UUID.randomUUID())
          .param("battle", battleId)
          .param("user", player.userId())
          .param("old", player.rating())
          .param("delta", delta)
          .param("new", player.rating().add(delta))
          .update();
      jdbc.sql(
              """
          UPDATE rating_profiles SET rating = :rating, ranked_games = ranked_games + 1,
            wins = wins + :win, losses = losses + :loss, draws = draws + :draw, updated_at = clock_timestamp()
          WHERE user_id = :user
          """)
          .param("rating", player.rating().add(delta))
          .param("user", player.userId())
          .param("win", score == 1 ? 1 : 0)
          .param("loss", score == 0 ? 1 : 0)
          .param("draw", score == 0.5 ? 1 : 0)
          .update();
    }
  }

  private record Before(UUID userId, BigDecimal rating, int games) {}
}
