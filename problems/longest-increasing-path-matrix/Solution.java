import java.util.*;

public class Solution {
  public int solve(int[][] matrix) {
    int rows = matrix.length;
    int columns = matrix[0].length;
    int[][] remaining = new int[rows][columns];
    int[] queue = new int[rows * columns];
    int[] dr = {-1, 1, 0, 0};
    int[] dc = {0, 0, -1, 1};
    int head = 0;
    int tail = 0;
    for (int row = 0; row < rows; row++) {
      for (int column = 0; column < columns; column++) {
        for (int direction = 0; direction < 4; direction++) {
          int nextRow = row + dr[direction];
          int nextColumn = column + dc[direction];
          if (nextRow >= 0 && nextRow < rows && nextColumn >= 0 && nextColumn < columns
              && matrix[nextRow][nextColumn] > matrix[row][column]) remaining[row][column]++;
        }
        if (remaining[row][column] == 0) queue[tail++] = row * columns + column;
      }
    }
    int length = 0;
    while (head < tail) {
      int end = tail;
      length++;
      while (head < end) {
        int cell = queue[head++];
        int row = cell / columns;
        int column = cell % columns;
        for (int direction = 0; direction < 4; direction++) {
          int nextRow = row + dr[direction];
          int nextColumn = column + dc[direction];
          if (nextRow >= 0 && nextRow < rows && nextColumn >= 0 && nextColumn < columns
              && matrix[nextRow][nextColumn] < matrix[row][column] && --remaining[nextRow][nextColumn] == 0) {
            queue[tail++] = nextRow * columns + nextColumn;
          }
        }
      }
    }
    return length;
  }
}
