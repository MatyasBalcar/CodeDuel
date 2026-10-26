import java.util.*;

public class Solution {
  public boolean solve(String note, String letters) {
    int[] available = new int[26];
    for (int index = 0; index < letters.length(); index++) available[letters.charAt(index) - 'a']++;
    for (int index = 0; index < note.length(); index++) {
      if (--available[note.charAt(index) - 'a'] < 0) return false;
    }
    return true;
  }
}
