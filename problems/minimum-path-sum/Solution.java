import java.util.*;

public class Solution {
  public long solve(int[][] grid) {
    long[] cost = new long[grid[0].length];
    Arrays.fill(cost, Long.MAX_VALUE);
    cost[0] = 0;
    for (int[] row : grid) {
      for (int column = 0; column < row.length; column++) {
        long fromLeft = column == 0 ? Long.MAX_VALUE : cost[column - 1];
        cost[column] = Math.min(cost[column], fromLeft) + row[column];
      }
    }
    return cost[cost.length - 1];
  }
}
