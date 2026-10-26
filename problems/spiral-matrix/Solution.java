import java.util.*;

public class Solution {
  public int[] solve(int[][] matrix) {
    int[] result = new int[matrix.length * matrix[0].length];
    int size = 0;
    int top = 0;
    int bottom = matrix.length - 1;
    int left = 0;
    int right = matrix[0].length - 1;
    while (top <= bottom && left <= right) {
      for (int column = left; column <= right; column++) result[size++] = matrix[top][column];
      top++;
      for (int row = top; row <= bottom; row++) result[size++] = matrix[row][right];
      right--;
      if (top <= bottom) {
        for (int column = right; column >= left; column--) result[size++] = matrix[bottom][column];
        bottom--;
      }
      if (left <= right) {
        for (int row = bottom; row >= top; row--) result[size++] = matrix[row][left];
        left++;
      }
    }
    return result;
  }
}
