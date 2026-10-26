import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    long[][] triangle = new long[count][];
    for (int row = 0; row < count; row++) {
      triangle[row] = new long[row + 1];
      for (int column = 0; column <= row; column++) triangle[row][column] = input.number();
    }
    long[] best = Arrays.copyOf(triangle[count - 1], count);
    for (int row = count - 2; row >= 0; row--) {
      for (int column = 0; column <= row; column++) {
        best[column] = triangle[row][column] + Math.min(best[column], best[column + 1]);
      }
    }
    System.out.println(best[0]);
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
