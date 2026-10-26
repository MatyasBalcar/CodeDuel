import java.util.*;

public class Solution {
  public int[] solve(int[] nums, long target) {
    Map<Long, Integer> firstPosition = new HashMap<>();
    int bestFirst = -1;
    int bestSecond = -1;
    for (int index = 0; index < nums.length; index++) {
      long value = nums[index];
      Integer first = firstPosition.get(target - value);
      if (first != null && (bestFirst == -1 || first < bestFirst)) {
        bestFirst = first;
        bestSecond = index;
      }
      firstPosition.putIfAbsent(value, index);
    }
    return bestFirst == -1 ? new int[0] : new int[] {bestFirst + 1, bestSecond + 1};
  }
}
