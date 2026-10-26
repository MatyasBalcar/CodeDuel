import java.util.*;

public class Solution {
  public long solve(int[] heights) {
    int left = 0;
    int right = heights.length - 1;
    long leftMax = 0;
    long rightMax = 0;
    long water = 0;
    while (left <= right) {
      if (heights[left] <= heights[right]) {
        leftMax = Math.max(leftMax, heights[left]);
        water += leftMax - heights[left++];
      } else {
        rightMax = Math.max(rightMax, heights[right]);
        water += rightMax - heights[right--];
      }
    }
    return water;
  }
}
