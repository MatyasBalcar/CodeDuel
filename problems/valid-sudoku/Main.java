import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    boolean[][] rows = new boolean[9][9];
    boolean[][] columns = new boolean[9][9];
    boolean[][] boxes = new boolean[9][9];
    boolean valid = true;
    for (int row = 0; row < 9; row++) {
      String cells = input.next();
      for (int column = 0; column < 9; column++) {
        if (cells.charAt(column) == '.') continue;
        int value = cells.charAt(column) - '1';
        int box = row / 3 * 3 + column / 3;
        if (rows[row][value] || columns[column][value] || boxes[box][value]) valid = false;
        rows[row][value] = columns[column][value] = boxes[box][value] = true;
      }
    }
    System.out.println(valid ? "YES" : "NO");
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
