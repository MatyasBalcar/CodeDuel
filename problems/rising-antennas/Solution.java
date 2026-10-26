import java.util.*;

public class Solution {
  public int solve(int[] heights) {
    int[] tails = new int[heights.length];
    int size = 0;
    for (int height : heights) {
      int left = 0;
      int right = size;
      while (left < right) {
        int middle = left + (right - left) / 2;
        if (tails[middle] < height) left = middle + 1;
        else right = middle;
      }
      tails[left] = height;
      if (left == size) size++;
    }
    return size;
  }
}
