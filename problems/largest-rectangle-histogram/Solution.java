import java.util.*;

public class Solution {
  public long solve(int[] heights) {
    int[] stack = new int[heights.length + 1];
    int size = 0;
    long best = 0;
    for (int index = 0; index <= heights.length; index++) {
      long height = index == heights.length ? 0 : heights[index];
      while (size > 0 && heights[stack[size - 1]] > height) {
        long previousHeight = heights[stack[--size]];
        int start = size == 0 ? 0 : stack[size - 1] + 1;
        best = Math.max(best, previousHeight * (index - start));
      }
      stack[size++] = index;
    }
    return best;
  }
}
