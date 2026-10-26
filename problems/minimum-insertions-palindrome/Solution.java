import java.util.*;

public class Solution {
  public int solve(String text) {
    int[] best = new int[text.length()];
    for (int left = text.length() - 2; left >= 0; left--) {
      int diagonal = 0;
      for (int right = left + 1; right < text.length(); right++) {
        int old = best[right];
        if (text.charAt(left) == text.charAt(right)) best[right] = diagonal;
        else best[right] = 1 + Math.min(best[right], best[right - 1]);
        diagonal = old;
      }
    }
    return text.isEmpty() ? 0 : best[text.length() - 1];
  }
}
