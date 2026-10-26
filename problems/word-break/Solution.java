import java.util.*;

public class Solution {
  public boolean solve(String text, String[] words) {
    Set<String> dictionary = new HashSet<>(Arrays.asList(words));
    int longest = 0;
    for (String word : words) longest = Math.max(longest, word.length());
    boolean[] possible = new boolean[text.length() + 1];
    possible[0] = true;
    for (int end = 1; end <= text.length(); end++) {
      for (int length = 1; length <= longest && length <= end; length++) {
        if (possible[end - length] && dictionary.contains(text.substring(end - length, end))) {
          possible[end] = true;
          break;
        }
      }
    }
    return possible[text.length()];
  }
}
