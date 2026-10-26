import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int rows = input.integer();
    int columns = input.integer();
    long[] cost = new long[columns];
    Arrays.fill(cost, Long.MAX_VALUE);
    cost[0] = 0;
    for (int row = 0; row < rows; row++) {
      for (int column = 0; column < columns; column++) {
        long value = input.number();
        long fromLeft = column == 0 ? Long.MAX_VALUE : cost[column - 1];
        cost[column] = Math.min(cost[column], fromLeft) + value;
      }
    }
    System.out.println(cost[columns - 1]);
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
