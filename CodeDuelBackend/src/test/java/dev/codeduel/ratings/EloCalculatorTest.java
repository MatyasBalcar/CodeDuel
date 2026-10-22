package dev.codeduel.ratings;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EloCalculatorTest {
  @Test
  void equalPlayersHaveEqualExpectation() {
    assertThat(EloCalculator.expectedScore(1200, 1200)).isEqualTo(0.5);
  }

  @Test
  void expectationMatchesPublishedFormula() {
    assertThat(EloCalculator.expectedScore(1400, 1500)).isCloseTo(0.359935, within(0.000001));
  }

  @Test
  void provisionalWinGainsTwentyAtEqualRatings() {
    assertThat(delta(1, 0)).isEqualByComparingTo("20.000000");
  }

  @Test
  void tenthGameIsStillProvisional() {
    assertThat(delta(0, 9)).isEqualByComparingTo("-20.000000");
  }

  @Test
  void eleventhGameUsesEstablishedK() {
    assertThat(delta(1, 10)).isEqualByComparingTo("12.000000");
  }

  @Test
  void equalDrawDoesNotChangeRatings() {
    assertThat(delta(0.5, 10)).isEqualByComparingTo("0.000000");
  }

  @Test
  void differentKValuesNeedNotBeZeroSum() {
    assertThat(delta(1, 0).add(delta(0, 10))).isEqualByComparingTo("8");
  }

  @Test
  void rejectsInvalidScoresAndNonfiniteRatings() {
    assertThatIllegalArgumentException().isThrownBy(() -> delta(0.7, 0));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EloCalculator.expectedScore(Double.NaN, 1200));
  }

  private BigDecimal delta(double score, int games) {
    return EloCalculator.delta(new BigDecimal("1200"), new BigDecimal("1200"), score, games);
  }
}
