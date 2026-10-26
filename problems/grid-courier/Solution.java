import java.util.*;

public class Solution {
  public int solve(String[] grid) {
    int rows = grid.length;
    int columns = grid[0].length();
    if (grid[0].charAt(0) == '#' || grid[rows - 1].charAt(columns - 1) == '#') return -1;
    int[] distance = new int[rows * columns];
    Arrays.fill(distance, -1);
    int[] queue = new int[rows * columns];
    int head = 0;
    int tail = 1;
    distance[0] = 0;
    int[] dr = {-1, 1, 0, 0};
    int[] dc = {0, 0, -1, 1};
    while (head < tail) {
      int current = queue[head++];
      int row = current / columns;
      int column = current % columns;
      for (int direction = 0; direction < 4; direction++) {
        int nr = row + dr[direction];
        int nc = column + dc[direction];
        if (nr < 0 || nr >= rows || nc < 0 || nc >= columns) continue;
        int next = nr * columns + nc;
        if (grid[nr].charAt(nc) != '#' && distance[next] == -1) {
          distance[next] = distance[current] + 1;
          queue[tail++] = next;
        }
      }
    }
    return distance[distance.length - 1];
  }
}
