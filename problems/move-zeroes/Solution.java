import java.util.*;

public class Solution {
  public int[] solve(int[] nums) {
    int[] result = new int[nums.length];
    int next = 0;
    for (int value : nums) if (value != 0) result[next++] = value;
    return result;
  }
}
