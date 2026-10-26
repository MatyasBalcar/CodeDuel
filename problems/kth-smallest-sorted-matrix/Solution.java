import java.util.*;

public class Solution {
  public int solve(int[][] matrix, int k) {
    int left = matrix[0][0];
    int right = matrix[matrix.length - 1][matrix.length - 1];
    while (left < right) {
      int middle = (int) (left + ((long) right - left) / 2);
      int count = 0;
      int column = matrix.length - 1;
      for (int[] row : matrix) {
        while (column >= 0 && row[column] > middle) column--;
        count += column + 1;
      }
      if (count < k) left = middle + 1;
      else right = middle;
    }
    return left;
  }
}
