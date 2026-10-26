import java.util.*;

public class Solution {
  public boolean solve(String first, String second) {
    if (first.length() != second.length()) return false;
    int[] counts = new int[26];
    for (int index = 0; index < first.length(); index++) {
      counts[first.charAt(index) - 'a']++;
      counts[second.charAt(index) - 'a']--;
    }
    for (int count : counts) if (count != 0) return false;
    return true;
  }
}
