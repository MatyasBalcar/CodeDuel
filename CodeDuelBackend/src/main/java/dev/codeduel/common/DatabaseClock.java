package dev.codeduel.common;

import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Wall-clock time is read after acquiring aggregate locks, never from a client. */
@Component
public class DatabaseClock {
  private final JdbcClient jdbc;

  public DatabaseClock(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public Instant now() {
    return jdbc.sql("SELECT clock_timestamp()")
        .query((rs, row) -> rs.getTimestamp(1).toInstant())
        .single();
  }
}
