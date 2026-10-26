import java.util.*;

public class Solution {
  public int[] solve(int[] nums) {
    int pivot = nums.length - 2;
    while (pivot >= 0 && nums[pivot] >= nums[pivot + 1]) pivot--;
    if (pivot >= 0) {
      int next = nums.length - 1;
      while (nums[next] <= nums[pivot]) next--;
      swap(nums, pivot, next);
    }
    int left = pivot + 1;
    int right = nums.length - 1;
    while (left < right) swap(nums, left++, right--);
    return nums;
  }

  private static void swap(int[] values, int first, int second) {
    int value = values[first];
    values[first] = values[second];
    values[second] = value;
  }
}
