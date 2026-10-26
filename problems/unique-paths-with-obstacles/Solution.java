import java.util.*;

public class Solution {
  public int solve(String[] grid) {
    int[] ways = new int[grid[0].length()];
    ways[0] = 1;
    for (String row : grid) {
      for (int column = 0; column < row.length(); column++) {
        if (row.charAt(column) == '#') ways[column] = 0;
        else if (column > 0) ways[column] = (ways[column] + ways[column - 1]) % 1000000007;
      }
    }
    return ways[ways.length - 1];
  }
}
