import java.util.*;

public class Solution {
  public boolean solve(String first, String second) {
    if (first.length() != second.length()) return false;
    int[] forward = new int[26];
    int[] backward = new int[26];
    Arrays.fill(forward, -1);
    Arrays.fill(backward, -1);
    for (int index = 0; index < first.length(); index++) {
      int from = first.charAt(index) - 'a';
      int to = second.charAt(index) - 'a';
      if ((forward[from] >= 0 && forward[from] != to) || (backward[to] >= 0 && backward[to] != from)) return false;
      forward[from] = to;
      backward[to] = from;
    }
    return true;
  }
}
