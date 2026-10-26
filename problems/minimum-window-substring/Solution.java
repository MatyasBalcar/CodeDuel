import java.util.*;

public class Solution {
  public String solve(String text, String target) {
    if (target.isEmpty()) return "";
    int[] needed = new int[128];
    for (int index = 0; index < target.length(); index++) needed[target.charAt(index)]++;
    int missing = target.length();
    int left = 0;
    int start = 0;
    int best = text.length() + 1;
    for (int right = 0; right < text.length(); right++) {
      if (needed[text.charAt(right)]-- > 0) missing--;
      while (missing == 0) {
        if (right - left + 1 < best) { best = right - left + 1; start = left; }
        if (++needed[text.charAt(left++)] > 0) missing++;
      }
    }
    return best > text.length() ? "" : text.substring(start, start + best);
  }
}
