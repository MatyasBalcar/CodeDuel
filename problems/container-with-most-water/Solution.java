import java.util.*;

public class Solution {
  public long solve(int[] heights) {
    int left = 0;
    int right = heights.length - 1;
    long best = 0;
    while (left < right) {
      best = Math.max(best, (long) (right - left) * Math.min(heights[left], heights[right]));
      if (heights[left] <= heights[right]) left++;
      else right--;
    }
    return best;
  }
}
