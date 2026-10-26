import java.util.*;

public class Solution {
  public long solve(int[] nums) {
    Arrays.sort(nums);
    long count = 0;
    for (int index = 0; index + 2 < nums.length; index++) {
      if (index > 0 && nums[index] == nums[index - 1]) continue;
      int left = index + 1;
      int right = nums.length - 1;
      while (left < right) {
        long sum = (long) nums[index] + nums[left] + nums[right];
        if (sum < 0) left++;
        else if (sum > 0) right--;
        else {
          count++;
          int first = nums[left];
          int last = nums[right];
          while (left < right && nums[left] == first) left++;
          while (left < right && nums[right] == last) right--;
        }
      }
    }
    return count;
  }
}
