import java.util.*;

public class Solution {
  public int solve(String text) {
    int best = 0;
    for (int center = 0; center < text.length(); center++) {
      best = Math.max(best, expand(text, center, center));
      best = Math.max(best, expand(text, center, center + 1));
    }
    return best;
  }

  private static int expand(String text, int left, int right) {
    while (left >= 0 && right < text.length() && text.charAt(left) == text.charAt(right)) {
      left--;
      right++;
    }
    return right - left - 1;
  }
}
