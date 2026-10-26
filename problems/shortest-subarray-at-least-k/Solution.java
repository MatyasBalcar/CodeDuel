import java.util.*;

public class Solution {
  public int solve(int[] nums, long target) {
    long[] prefix = new long[nums.length + 1];
    for (int index = 0; index < nums.length; index++) prefix[index + 1] = prefix[index] + nums[index];
    int[] queue = new int[prefix.length];
    int head = 0;
    int tail = 0;
    int best = nums.length + 1;
    for (int end = 0; end < prefix.length; end++) {
      while (head < tail && prefix[end] - prefix[queue[head]] >= target) best = Math.min(best, end - queue[head++]);
      while (head < tail && prefix[queue[tail - 1]] >= prefix[end]) tail--;
      queue[tail++] = end;
    }
    return best > nums.length ? -1 : best;
  }
}
