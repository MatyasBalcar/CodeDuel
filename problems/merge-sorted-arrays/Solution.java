import java.util.*;

public class Solution {
  public int[] solve(int[] first, int[] second) {
    int[] merged = new int[first.length + second.length];
    int left = 0;
    int right = 0;
    for (int index = 0; index < merged.length; index++) {
      if (right == second.length || (left < first.length && first[left] <= second[right])) merged[index] = first[left++];
      else merged[index] = second[right++];
    }
    return merged;
  }
}
