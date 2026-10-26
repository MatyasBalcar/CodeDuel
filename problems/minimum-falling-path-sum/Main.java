import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    long[] previous = new long[count];
    for (int row = 0; row < count; row++) {
      long[] current = new long[count];
      for (int column = 0; column < count; column++) {
        long best = previous[column];
        if (column > 0) best = Math.min(best, previous[column - 1]);
        if (column + 1 < count) best = Math.min(best, previous[column + 1]);
        current[column] = best + input.number();
      }
      previous = current;
    }
    long best = previous[0];
    for (long value : previous) best = Math.min(best, value);
    System.out.println(best);
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
