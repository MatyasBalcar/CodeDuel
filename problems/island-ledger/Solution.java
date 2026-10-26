import java.util.*;

public class Solution {
  public int[] solve(String[] grid) {
    int rows = grid.length;
    int columns = grid[0].length();
    boolean[][] seen = new boolean[rows][columns];
    int[] queue = new int[rows * columns];
    int[] dr = {-1, 1, 0, 0};
    int[] dc = {0, 0, -1, 1};
    int islands = 0;
    int largest = 0;
    for (int row = 0; row < rows; row++) {
      for (int column = 0; column < columns; column++) {
        if (seen[row][column] || grid[row].charAt(column) != '1') continue;
        islands++;
        int head = 0;
        int tail = 1;
        queue[0] = row * columns + column;
        seen[row][column] = true;
        while (head < tail) {
          int current = queue[head++];
          int cr = current / columns;
          int cc = current % columns;
          for (int direction = 0; direction < 4; direction++) {
            int nr = cr + dr[direction];
            int nc = cc + dc[direction];
            if (nr < 0 || nr >= rows || nc < 0 || nc >= columns) continue;
            if (!seen[nr][nc] && grid[nr].charAt(nc) == '1') {
              seen[nr][nc] = true;
              queue[tail++] = nr * columns + nc;
            }
          }
        }
        largest = Math.max(largest, tail);
      }
    }
    return new int[] {islands, largest};
  }
}
