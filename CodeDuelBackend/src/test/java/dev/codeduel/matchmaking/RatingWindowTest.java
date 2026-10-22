package dev.codeduel.matchmaking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RatingWindowTest {
  private static final Instant START = Instant.parse("2026-10-20T20:00:00Z");

  @Test
  void startsAt150() {
    assertThat(RatingWindow.at(START, START)).isEqualTo(150);
  }

  @Test
  void expandsAtTenSeconds() {
    assertThat(RatingWindow.at(START, START.plusSeconds(9))).isEqualTo(150);
    assertThat(RatingWindow.at(START, START.plusSeconds(10))).isEqualTo(200);
  }

  @Test
  void capAndClockSkewAreBounded() {
    assertThat(RatingWindow.at(START, START.plusSeconds(10000))).isEqualTo(600);
    assertThat(RatingWindow.at(START, START.minusSeconds(10))).isEqualTo(150);
  }
}
