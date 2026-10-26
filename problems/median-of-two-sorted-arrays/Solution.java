import java.util.*;

public class Solution {
  public double solve(int[] first, int[] second) {
    if (first.length > second.length) return solve(second, first);
    int left = 0;
    int right = first.length;
    int half = (first.length + second.length + 1) / 2;
    while (left <= right) {
      int a = (left + right) / 2;
      int b = half - a;
      int beforeA = a == 0 ? Integer.MIN_VALUE : first[a - 1];
      int afterA = a == first.length ? Integer.MAX_VALUE : first[a];
      int beforeB = b == 0 ? Integer.MIN_VALUE : second[b - 1];
      int afterB = b == second.length ? Integer.MAX_VALUE : second[b];
      if (beforeA > afterB) right = a - 1;
      else if (beforeB > afterA) left = a + 1;
      else {
        if ((first.length + second.length) % 2 == 1) return Math.max(beforeA, beforeB);
        return ((long) Math.max(beforeA, beforeB) + Math.min(afterA, afterB)) / 2.0;
      }
    }
    throw new IllegalArgumentException("Arrays must be sorted");
  }
}
