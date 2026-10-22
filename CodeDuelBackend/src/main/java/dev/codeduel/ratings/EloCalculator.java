package dev.codeduel.ratings;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Internal storage uses six decimal places. Public ratings use HALF_UP integer rounding. */
public final class EloCalculator {
  private EloCalculator() {}

  public static double expectedScore(double player, double opponent) {
    if (!Double.isFinite(player) || !Double.isFinite(opponent))
      throw new IllegalArgumentException("Ratings must be finite");
    return 1.0 / (1.0 + Math.pow(10, (opponent - player) / 400.0));
  }

  public static BigDecimal delta(BigDecimal player, BigDecimal opponent, double score, int games) {
    if (score != 0 && score != 0.5 && score != 1)
      throw new IllegalArgumentException("Invalid score");
    if (games < 0) throw new IllegalArgumentException("Invalid games");
    int k = games < 10 ? 40 : 24;
    return BigDecimal.valueOf(
            k * (score - expectedScore(player.doubleValue(), opponent.doubleValue())))
        .setScale(6, RoundingMode.HALF_UP);
  }
}
