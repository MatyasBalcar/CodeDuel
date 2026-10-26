import java.util.*;

public class Solution {
  public int solve(int[] keys) {
    int left = 0;
    int right = keys.length - 1;
    while (left < right) {
      int middle = left + (right - left) / 2;
      if (keys[middle] > keys[right]) left = middle + 1;
      else right = middle;
    }
    return left;
  }
}
