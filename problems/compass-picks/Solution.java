import java.util.*;

public class Solution {
  public int[] solve(int[][] points, int k) {
    Integer[] order = new Integer[points.length];
    for (int index = 0; index < points.length; index++) order[index] = index;
    Arrays.sort(order, Comparator.<Integer>comparingLong(index -> distance(points[index]))
        .thenComparingInt(index -> index));
    int[] chosen = new int[k];
    for (int index = 0; index < k; index++) chosen[index] = order[index] + 1;
    return chosen;
  }

  private static long distance(int[] point) {
    return (long) point[0] * point[0] + (long) point[1] * point[1];
  }
}
