import java.util.*;

public class Solution {
  public int solve(String first, String second) {
    int[] previous = new int[second.length() + 1];
    int[] current = new int[second.length() + 1];
    for (int row = 1; row <= first.length(); row++) {
      for (int column = 1; column <= second.length(); column++) {
        if (first.charAt(row - 1) == second.charAt(column - 1)) current[column] = previous[column - 1] + 1;
        else current[column] = Math.max(previous[column], current[column - 1]);
      }
      int[] swap = previous;
      previous = current;
      current = swap;
    }
    return previous[second.length()];
  }
}
