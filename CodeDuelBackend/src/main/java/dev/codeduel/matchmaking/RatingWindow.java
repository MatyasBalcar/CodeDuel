package dev.codeduel.matchmaking;

import java.time.Duration;
import java.time.Instant;

public final class RatingWindow {
  private RatingWindow() {}

  public static int at(Instant queuedAt, Instant now) {
    long waited = Math.max(0, Duration.between(queuedAt, now).getSeconds());
    return (int) Math.min(600, 150 + 50 * (waited / 10));
  }
}
