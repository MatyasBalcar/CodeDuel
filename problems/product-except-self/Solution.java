import java.util.*;

public class Solution {
  public long[] solve(int[] nums) {
    long[] result = new long[nums.length];
    long product = 1;
    for (int index = 0; index < nums.length; index++) {
      result[index] = product;
      product *= nums[index];
    }
    product = 1;
    for (int index = nums.length - 1; index >= 0; index--) {
      result[index] *= product;
      product *= nums[index];
    }
    return result;
  }
}
