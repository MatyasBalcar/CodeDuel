import java.util.*;

public class Solution {
  public int solve(String text, int k) {
    int[] counts = new int[26];
    int left = 0;
    int most = 0;
    int best = 0;
    for (int right = 0; right < text.length(); right++) {
      most = Math.max(most, ++counts[text.charAt(right) - 'A']);
      while (right - left + 1 - most > k) counts[text.charAt(left++) - 'A']--;
      best = Math.max(best, right - left + 1);
    }
    return best;
  }
}
