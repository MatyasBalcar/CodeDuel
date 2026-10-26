import java.util.*;

public class Solution {
  public long solve(int[][] matrix) {
    long total = 0;
    for (int row = 0; row < matrix.length; row++) {
      total += matrix[row][row];
      int opposite = matrix.length - row - 1;
      if (row != opposite) total += matrix[row][opposite];
    }
    return total;
  }
}
