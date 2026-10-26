import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int rows = input.integer();
    int columns = input.integer();
    char[][] grid = new char[rows][];
    int[] available = new int[26];
    for (int row = 0; row < rows; row++) {
      grid[row] = input.next().toCharArray();
      for (char value : grid[row]) available[value - 'A']++;
    }
    String word = input.next();
    int[] needed = new int[26];
    for (int index = 0; index < word.length(); index++) needed[word.charAt(index) - 'A']++;
    for (int value = 0; value < 26; value++) {
      if (needed[value] > available[value]) {
        System.out.println("NO");
        return;
      }
    }
    for (int row = 0; row < rows; row++) {
      for (int column = 0; column < columns; column++) {
        if (search(grid, word, row, column, 0)) {
          System.out.println("YES");
          return;
        }
      }
    }
    System.out.println("NO");
  }

  private static boolean search(char[][] grid, String word, int row, int column, int index) {
    if (index == word.length()) return true;
    if (row < 0 || row >= grid.length || column < 0 || column >= grid[0].length) return false;
    if (grid[row][column] != word.charAt(index)) return false;
    char value = grid[row][column];
    grid[row][column] = 0;
    boolean found = search(grid, word, row - 1, column, index + 1)
        || search(grid, word, row + 1, column, index + 1)
        || search(grid, word, row, column - 1, index + 1)
        || search(grid, word, row, column + 1, index + 1);
    grid[row][column] = value;
    return found;
  }

  private static final class Input {
    private final byte[] buffer = new byte[65536];
    private int offset;
    private int size;

    private int read() throws IOException {
      if (offset >= size) {
        size = System.in.read(buffer);
        offset = 0;
        if (size < 0) return -1;
      }
      return buffer[offset++];
    }

    String next() throws IOException {
      StringBuilder value = new StringBuilder();
      int c;
      do {
        c = read();
      } while (c >= 0 && c <= 32);
      while (c > 32) {
        value.append((char) c);
        c = read();
      }
      return value.toString();
    }

    int integer() throws IOException {
      return Integer.parseInt(next());
    }

    long number() throws IOException {
      return Long.parseLong(next());
    }
  }
}
