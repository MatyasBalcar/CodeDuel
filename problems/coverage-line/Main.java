import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int n = input.integer();
    long[][] intervals = new long[n][2];
    for (int i = 0; i < n; i++) {
      intervals[i][0] = input.number();
      intervals[i][1] = input.number();
    }
    Arrays.sort(intervals, Comparator.comparingLong(a -> a[0]));
    long total = 0, left = 0, right = 0;
    boolean open = false;
    for (long[] interval : intervals) {
      if (!open || interval[0] > right) {
        if (open) total += right - left;
        left = interval[0];
        right = interval[1];
        open = true;
      } else right = Math.max(right, interval[1]);
    }
    if (open) total += right - left;
    System.out.println(total);
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
