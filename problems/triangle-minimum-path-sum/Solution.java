import java.util.*;

public class Solution {
  public long solve(int[][] triangle) {
    long[] best = new long[triangle.length];
    for (int column = 0; column < best.length; column++) best[column] = triangle[triangle.length - 1][column];
    for (int row = triangle.length - 2; row >= 0; row--) {
      for (int column = 0; column <= row; column++) best[column] = triangle[row][column] + Math.min(best[column], best[column + 1]);
    }
    return best[0];
  }
}
