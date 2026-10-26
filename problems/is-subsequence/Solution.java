import java.util.*;

public class Solution {
  public boolean solve(String subsequence, String text) {
    int matched = 0;
    for (int index = 0; index < text.length(); index++) {
      if (matched < subsequence.length() && subsequence.charAt(matched) == text.charAt(index)) matched++;
    }
    return matched == subsequence.length();
  }
}
