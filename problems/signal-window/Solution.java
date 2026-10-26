import java.util.*;

public class Solution {
  public int solve(String signal) {
    int[] previous = new int[128];
    Arrays.fill(previous, -1);
    int left = 0;
    int best = 0;
    for (int right = 0; right < signal.length(); right++) {
      char value = signal.charAt(right);
      left = Math.max(left, previous[value] + 1);
      best = Math.max(best, right - left + 1);
      previous[value] = right;
    }
    return best;
  }
}
