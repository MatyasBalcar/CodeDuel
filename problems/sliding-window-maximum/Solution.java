import java.util.*;

public class Solution {
  public int[] solve(int[] nums, int k) {
    int[] queue = new int[nums.length];
    int[] answer = new int[nums.length - k + 1];
    int head = 0;
    int tail = 0;
    for (int index = 0; index < nums.length; index++) {
      while (head < tail && queue[head] <= index - k) head++;
      while (head < tail && nums[queue[tail - 1]] <= nums[index]) tail--;
      queue[tail++] = index;
      if (index >= k - 1) answer[index - k + 1] = nums[queue[head]];
    }
    return answer;
  }
}
