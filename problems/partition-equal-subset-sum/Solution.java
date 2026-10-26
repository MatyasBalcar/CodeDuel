import java.util.*;

public class Solution {
  public boolean solve(int[] nums) {
    int total = 0;
    for (int value : nums) total += value;
    if (total % 2 != 0) return false;
    int target = total / 2;
    boolean[] possible = new boolean[target + 1];
    possible[0] = true;
    for (int value : nums) {
      for (int sum = target; sum >= value; sum--) possible[sum] |= possible[sum - value];
    }
    return possible[target];
  }
}
