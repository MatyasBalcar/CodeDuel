import java.util.*;

public class Solution {
  public long solve(int[] nums, long k) {
    if (k <= 1) return 0;
    long product = 1;
    long count = 0;
    int left = 0;
    for (int right = 0; right < nums.length; right++) {
      product *= nums[right];
      while (left <= right && product >= k) product /= nums[left++];
      count += right - left + 1;
    }
    return count;
  }
}
