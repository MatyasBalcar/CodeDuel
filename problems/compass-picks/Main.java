import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int n = input.integer(), k = input.integer();
    long[][] stations = new long[n][2];
    for (int i = 0; i < n; i++) {
      long x = input.number(), y = input.number();
      stations[i][0] = x * x + y * y;
      stations[i][1] = i + 1;
    }
    Arrays.sort(stations, Comparator.<long[]>comparingLong(a -> a[0]).thenComparingLong(a -> a[1]));
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < k; i++) {
      if (i > 0) result.append(' ');
      result.append(stations[i][1]);
    }
    System.out.println(result);
  }

  private static final class Input {
    private final byte[] buffer = new byte[65536];
    private int offset, size;

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
