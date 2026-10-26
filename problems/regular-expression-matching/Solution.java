import java.util.*;

public class Solution {
  public boolean solve(String text, String pattern) {
    boolean[] previous = new boolean[pattern.length() + 1];
    previous[0] = true;
    for (int column = 2; column <= pattern.length(); column++) {
      if (pattern.charAt(column - 1) == '*') previous[column] = previous[column - 2];
    }
    for (int row = 1; row <= text.length(); row++) {
      boolean[] current = new boolean[pattern.length() + 1];
      for (int column = 1; column <= pattern.length(); column++) {
        char token = pattern.charAt(column - 1);
        if (token == '*') {
          char repeated = pattern.charAt(column - 2);
          current[column] = current[column - 2] || ((repeated == '.' || repeated == text.charAt(row - 1)) && previous[column]);
        } else current[column] = previous[column - 1] && (token == '.' || token == text.charAt(row - 1));
      }
      previous = current;
    }
    return previous[pattern.length()];
  }
}
