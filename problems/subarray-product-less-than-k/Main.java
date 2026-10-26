import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    long limit = input.number();
    long[] values = new long[count];
    for (int index = 0; index < count; index++) values[index] = input.number();
    if (limit <= 1) {
      System.out.println(0);
      return;
    }
    int left = 0;
    long product = 1;
    long answer = 0;
    for (int right = 0; right < count; right++) {
      product *= values[right];
      while (product >= limit) product /= values[left++];
      answer += right - left + 1;
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
