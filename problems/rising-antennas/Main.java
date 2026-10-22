import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int n = input.integer(), length = 0;
    int[] tails = new int[n];
    for (int i = 0; i < n; i++) {
      int value = input.integer(), low = 0, high = length;
      while (low < high) {
        int mid = (low + high) >>> 1;
        if (tails[mid] < value) low = mid + 1;
        else high = mid;
      }
      tails[low] = value;
      if (low == length) length++;
    }
    System.out.println(length);
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
