import java.util.*;

public class Solution {
  public boolean solve(int[] nums) {
    Set<Integer> seen = new HashSet<>();
    for (int value : nums) if (!seen.add(value)) return true;
    return false;
  }
}
