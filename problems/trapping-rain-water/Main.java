import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    long[] heights = new long[count];
    for (int index = 0; index < count; index++) heights[index] = input.number();
    int left = 0;
    int right = count - 1;
    long leftMax = 0;
    long rightMax = 0;
    long water = 0;
    while (left <= right) {
      if (heights[left] <= heights[right]) {
        leftMax = Math.max(leftMax, heights[left]);
        water += leftMax - heights[left++];
      } else {
        rightMax = Math.max(rightMax, heights[right]);
        water += rightMax - heights[right--];
      }
    }
    System.out.println(water);
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
