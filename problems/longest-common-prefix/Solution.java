import java.util.*;

public class Solution {
  public String solve(String[] words) {
    if (words.length == 0) return "";
    String first = words[0];
    int length = first.length();
    for (int index = 1; index < words.length; index++) {
      length = Math.min(length, words[index].length());
      int matched = 0;
      while (matched < length && first.charAt(matched) == words[index].charAt(matched)) matched++;
      length = matched;
    }
    return first.substring(0, length);
  }
}
