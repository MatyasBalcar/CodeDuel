import java.util.*;

public class Solution {
  public boolean solve(String[] board) {
    boolean[][] rows = new boolean[9][9];
    boolean[][] columns = new boolean[9][9];
    boolean[][] boxes = new boolean[9][9];
    for (int row = 0; row < 9; row++) {
      for (int column = 0; column < 9; column++) {
        char cell = board[row].charAt(column);
        if (cell == '.') continue;
        int value = cell - '1';
        int box = row / 3 * 3 + column / 3;
        if (rows[row][value] || columns[column][value] || boxes[box][value]) return false;
        rows[row][value] = columns[column][value] = boxes[box][value] = true;
      }
    }
    return true;
  }
}
