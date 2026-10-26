import java.util.*;

public class Solution {
  public long[] solve(int[] nums) {
    long[] result = new long[nums.length];
    int left = 0;
    int right = nums.length - 1;
    for (int index = result.length - 1; index >= 0; index--) {
      long first = (long) nums[left] * nums[left];
      long last = (long) nums[right] * nums[right];
      if (first > last) {
        result[index] = first;
        left++;
      } else {
        result[index] = last;
        right--;
      }
    }
    return result;
  }
}
