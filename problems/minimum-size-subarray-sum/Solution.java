import java.util.*;

public class Solution {
  public int solve(int[] nums, long target) {
    int left = 0;
    int best = nums.length + 1;
    long sum = 0;
    for (int right = 0; right < nums.length; right++) {
      sum += nums[right];
      while (sum >= target) {
        best = Math.min(best, right - left + 1);
        sum -= nums[left++];
      }
    }
    return best > nums.length ? 0 : best;
  }
}
