import java.util.*;

public class Solution {
  public int[] solve(int[] keys, int[] queries) {
    int[] answers = new int[queries.length];
    for (int index = 0; index < queries.length; index++) {
      int left = 0;
      int right = keys.length;
      while (left < right) {
        int middle = left + (right - left) / 2;
        if (keys[middle] < queries[index]) left = middle + 1;
        else right = middle;
      }
      answers[index] = left == keys.length ? -1 : left;
    }
    return answers;
  }
}
