import java.util.*;

public class Solution {
  public long solve(int[] values, long target) {
    Map<Long, Long> prefixes = new HashMap<>();
    prefixes.put(0L, 1L);
    long sum = 0;
    long count = 0;
    for (int value : values) {
      sum += value;
      count += prefixes.getOrDefault(sum - target, 0L);
      prefixes.merge(sum, 1L, Long::sum);
    }
    return count;
  }
}
