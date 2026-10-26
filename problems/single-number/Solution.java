import java.util.*;

public class Solution {
  public int solve(int[] nums) {
    int answer = 0;
    for (int value : nums) answer ^= value;
    return answer;
  }
}
