package dev.codeduel.judge;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class JudgeMetrics implements MeterBinder {
  private final JdbcClient jdbc;

  public JudgeMetrics(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    for (String state : new String[] {"PENDING", "RUNNING", "DEAD"}) {
      Gauge.builder(
              "codeduel.judge.jobs",
              () -> value("SELECT count(*) FROM judge_jobs WHERE status = '" + state + "'"))
          .tag("state", state)
          .register(registry);
    }
    Gauge.builder(
            "codeduel.judge.oldest.seconds",
            () ->
                value(
                    "SELECT COALESCE(extract(epoch FROM clock_timestamp() - min(created_at)),0) FROM judge_jobs WHERE status IN ('PENDING','RUNNING')"))
        .register(registry);
  }

  private double value(String sql) {
    try {
      return jdbc.sql(sql).query(Double.class).single();
    } catch (RuntimeException unavailable) {
      return Double.NaN;
    }
  }
}
