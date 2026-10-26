import java.util.*;

public class Solution {
  public long solve(int[][] matrix) {
    long[] previous = new long[matrix.length];
    for (int[] row : matrix) {
      long[] current = new long[matrix.length];
      for (int column = 0; column < row.length; column++) {
        long best = previous[column];
        if (column > 0) best = Math.min(best, previous[column - 1]);
        if (column + 1 < row.length) best = Math.min(best, previous[column + 1]);
        current[column] = best + row[column];
      }
      previous = current;
    }
    long best = previous[0];
    for (long value : previous) best = Math.min(best, value);
    return best;
  }
}
