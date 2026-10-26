import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    long[] values = new long[count + 2];
    values[0] = values[count + 1] = 1;
    for (int index = 1; index <= count; index++) values[index] = input.number();
    long[][] best = new long[count + 2][count + 2];
    for (int length = 1; length <= count; length++) {
      for (int left = 1; left + length - 1 <= count; left++) {
        int right = left + length - 1;
        for (int last = left; last <= right; last++) {
          long score = best[left][last - 1] + best[last + 1][right]
              + values[left - 1] * values[last] * values[right + 1];
          best[left][right] = Math.max(best[left][right], score);
        }
      }
    }
    System.out.println(best[1][count]);
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
