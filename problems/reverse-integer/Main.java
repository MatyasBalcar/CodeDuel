import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    long value = input.number();
    long sign = value < 0 ? -1 : 1;
    value = Math.abs(value);
    long reversed = 0;
    while (value > 0) {
      reversed = reversed * 10 + value % 10;
      value /= 10;
    }
    reversed *= sign;
    System.out.println(reversed < Integer.MIN_VALUE || reversed > Integer.MAX_VALUE ? 0 : reversed);
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
