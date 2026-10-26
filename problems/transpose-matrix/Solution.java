import java.util.*;

public class Solution {
  public int[][] solve(int[][] matrix) {
    int[][] result = new int[matrix[0].length][matrix.length];
    for (int row = 0; row < matrix.length; row++) {
      for (int column = 0; column < matrix[row].length; column++) result[column][row] = matrix[row][column];
    }
    return result;
  }
}
