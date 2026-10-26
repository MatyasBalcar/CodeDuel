import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    long[] heights = new long[count];
    for (int index = 0; index < count; index++) heights[index] = input.number();
    int[] stack = new int[count + 1];
    int size = 0;
    long best = 0;
    for (int index = 0; index <= count; index++) {
      long height = index == count ? 0 : heights[index];
      while (size > 0 && heights[stack[size - 1]] > height) {
        long previousHeight = heights[stack[--size]];
        int start = size == 0 ? 0 : stack[size - 1] + 1;
        best = Math.max(best, previousHeight * (index - start));
      }
      stack[size++] = index;
    }
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
