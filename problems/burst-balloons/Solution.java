import java.util.*;

public class Solution {
  public long solve(int[] nums) {
    int[] values = new int[nums.length + 2];
    values[0] = values[values.length - 1] = 1;
    System.arraycopy(nums, 0, values, 1, nums.length);
    long[][] best = new long[values.length][values.length];
    for (int width = 2; width < values.length; width++) {
      for (int left = 0; left + width < values.length; left++) {
        int right = left + width;
        for (int last = left + 1; last < right; last++) {
          long score = best[left][last] + best[last][right] + (long) values[left] * values[last] * values[right];
          best[left][right] = Math.max(best[left][right], score);
        }
      }
    }
    return best[0][values.length - 1];
  }
}
