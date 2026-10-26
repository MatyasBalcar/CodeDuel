import java.util.*;

public class Solution {
  public int solve(int[] nums) {
    long missing = (long) nums.length * (nums.length + 1) / 2;
    for (int value : nums) missing -= value;
    return (int) missing;
  }
}
