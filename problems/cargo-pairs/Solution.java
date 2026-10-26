import java.util.*;

public class Solution {
  public long solve(int[] weights, long target) {
    Map<Long, Long> seen = new HashMap<>();
    long pairs = 0;
    for (long weight : weights) {
      pairs += seen.getOrDefault(target - weight, 0L);
      seen.merge(weight, 1L, Long::sum);
    }
    return pairs;
  }
}
