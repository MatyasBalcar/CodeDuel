import java.util.*;

public class Solution {
  public int solve(String text) {
    int[] counts = new int[26];
    for (int index = 0; index < text.length(); index++) counts[text.charAt(index) - 'a']++;
    for (int index = 0; index < text.length(); index++) {
      if (counts[text.charAt(index) - 'a'] == 1) return index;
    }
    return -1;
  }
}
