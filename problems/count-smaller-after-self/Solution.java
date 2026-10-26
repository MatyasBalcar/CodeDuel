import java.util.*;

public class Solution {
  public int[] solve(int[] nums) {
    int[] sorted = Arrays.copyOf(nums, nums.length);
    Arrays.sort(sorted);
    int[] tree = new int[nums.length + 1];
    int[] result = new int[nums.length];
    for (int index = nums.length - 1; index >= 0; index--) {
      int rank = lowerBound(sorted, nums[index]) + 1;
      for (int position = rank - 1; position > 0; position -= position & -position) result[index] += tree[position];
      for (int position = rank; position < tree.length; position += position & -position) tree[position]++;
    }
    return result;
  }

  private static int lowerBound(int[] values, int target) {
    int left = 0;
    int right = values.length;
    while (left < right) {
      int middle = (left + right) / 2;
      if (values[middle] < target) left = middle + 1;
      else right = middle;
    }
    return left;
  }
}
