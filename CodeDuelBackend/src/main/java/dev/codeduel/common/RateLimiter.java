package dev.codeduel.common;

import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Durable fixed windows. Rejected requests must still count when their caller rolls back. */
@Service
public class RateLimiter {
  private final JdbcClient jdbc;
  private final TransactionTemplate transaction;

  public RateLimiter(JdbcClient jdbc, PlatformTransactionManager manager) {
    this.jdbc = jdbc;
    transaction = new TransactionTemplate(manager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public void check(String key, int limit, Duration window) {
    Integer hits =
        transaction.execute(
            status ->
                jdbc.sql(
                        """
        INSERT INTO request_limits(bucket_key, hits, expires_at)
        VALUES (:key, 1, clock_timestamp() + :seconds * interval '1 second')
        ON CONFLICT (bucket_key) DO UPDATE SET
          hits = CASE WHEN request_limits.expires_at <= clock_timestamp() THEN 1 ELSE request_limits.hits + 1 END,
          expires_at = CASE WHEN request_limits.expires_at <= clock_timestamp()
            THEN clock_timestamp() + :seconds * interval '1 second' ELSE request_limits.expires_at END
        RETURNING hits
        """)
                    .param("key", key)
                    .param("seconds", window.toSeconds())
                    .query(Integer.class)
                    .single());
    if (hits == null || hits > limit) {
      throw new ApiException(
          HttpStatus.TOO_MANY_REQUESTS,
          "RATE_LIMITED",
          "Too many requests. Please wait a moment and try again.");
    }
  }
}
