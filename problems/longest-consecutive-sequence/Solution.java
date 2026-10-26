import java.util.*;

public class Solution {
  public int solve(int[] nums) {
    Set<Integer> values = new HashSet<>();
    for (int value : nums) values.add(value);
    int best = 0;
    for (int value : values) {
      if (values.contains(value - 1)) continue;
      int length = 1;
      while (values.contains(value + length)) length++;
      best = Math.max(best, length);
    }
    return best;
  }
}
