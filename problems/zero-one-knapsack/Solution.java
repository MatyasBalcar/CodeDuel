import java.util.*;

public class Solution {
  public long solve(int[] weights, int[] values, int capacity) {
    long[] best = new long[capacity + 1];
    for (int index = 0; index < weights.length; index++) {
      for (int weight = capacity; weight >= weights[index]; weight--) {
        best[weight] = Math.max(best[weight], best[weight - weights[index]] + values[index]);
      }
    }
    return best[capacity];
  }
}
