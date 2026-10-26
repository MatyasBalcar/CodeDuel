import java.util.*;

public class Solution {
  public int solve(int[] nums) {
    int candidate = 0;
    int votes = 0;
    for (int value : nums) {
      if (votes == 0) candidate = value;
      votes += value == candidate ? 1 : -1;
    }
    return candidate;
  }
}
