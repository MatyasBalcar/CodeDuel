import java.util.*;

public class Solution {
  public boolean solve(String[] board, String word) {
    if (word.isEmpty()) return true;
    char[][] cells = new char[board.length][];
    for (int row = 0; row < board.length; row++) cells[row] = board[row].toCharArray();
    for (int row = 0; row < cells.length; row++) {
      for (int column = 0; column < cells[row].length; column++) {
        if (search(cells, word, row, column, 0)) return true;
      }
    }
    return false;
  }

  private static boolean search(char[][] board, String word, int row, int column, int index) {
    if (index == word.length()) return true;
    if (row < 0 || row >= board.length || column < 0 || column >= board[row].length
        || board[row][column] != word.charAt(index)) return false;
    char value = board[row][column];
    board[row][column] = '\0';
    boolean found = search(board, word, row + 1, column, index + 1)
        || search(board, word, row - 1, column, index + 1)
        || search(board, word, row, column + 1, index + 1)
        || search(board, word, row, column - 1, index + 1);
    board[row][column] = value;
    return found;
  }
}
