import java.util.*;

public class Solution {
  public int solve(String text) {
    if (text.isEmpty()) return 0;
    boolean[][] palindrome = new boolean[text.length()][text.length()];
    int[] cuts = new int[text.length() + 1];
    cuts[0] = -1;
    for (int end = 0; end < text.length(); end++) {
      cuts[end + 1] = end;
      for (int start = 0; start <= end; start++) {
        if (text.charAt(start) == text.charAt(end) && (end - start < 2 || palindrome[start + 1][end - 1])) {
          palindrome[start][end] = true;
          cuts[end + 1] = Math.min(cuts[end + 1], cuts[start] + 1);
        }
      }
    }
    return cuts[text.length()];
  }
}
