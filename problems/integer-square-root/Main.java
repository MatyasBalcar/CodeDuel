import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    long value = input.number();
    long left = 0;
    long right = Math.min(value, 1000000000L);
    long answer = 0;
    while (left <= right) {
      long middle = left + (right - left) / 2;
      if (middle == 0 || middle <= value / middle) {
        answer = middle;
        left = middle + 1;
      } else {
        right = middle - 1;
      }
    }
    System.out.println(answer);
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
