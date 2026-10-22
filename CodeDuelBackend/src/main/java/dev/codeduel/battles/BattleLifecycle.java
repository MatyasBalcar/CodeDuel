package dev.codeduel.battles;

import dev.codeduel.matchmaking.MatchmakingService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "codeduel.scheduling-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class BattleLifecycle {
  private static final Logger LOG = LoggerFactory.getLogger(BattleLifecycle.class);
  private final JdbcClient jdbc;
  private final MatchmakingService matchmaking;
  private final FinalizeBattleService finalizer;

  public BattleLifecycle(
      JdbcClient jdbc, MatchmakingService matchmaking, FinalizeBattleService finalizer) {
    this.jdbc = jdbc;
    this.matchmaking = matchmaking;
    this.finalizer = finalizer;
  }

  @Scheduled(fixedDelay = 1000, initialDelay = 3000)
  public void tick() {
    matchmaking.pair();
    var due =
        jdbc.sql(
                """
        SELECT id FROM battles WHERE
          (status IN ('WAITING','READY_CHECK') AND ready_deadline <= clock_timestamp())
          OR (status IN ('IN_PROGRESS','JUDGING') AND ends_at <= clock_timestamp())
        ORDER BY created_at LIMIT 100
        """)
            .query(UUID.class)
            .list();
    for (UUID id : due) {
      try {
        finalizer.attempt(id);
      } catch (RuntimeException ex) {
        LOG.warn("Battle reconciliation failed for {} ({})", id, ex.getClass().getSimpleName());
      }
    }
  }

  @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
  public void cleanupLimits() {
    jdbc.sql("DELETE FROM request_limits WHERE expires_at < clock_timestamp() - interval '1 hour'")
        .update();
  }
}
