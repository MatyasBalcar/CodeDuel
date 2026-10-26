import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    int[] values = new int[count];
    int total = 0;
    for (int index = 0; index < count; index++) {
      values[index] = input.integer();
      total += values[index];
    }
    if (total % 2 != 0) {
      System.out.println("NO");
      return;
    }
    int target = total / 2;
    boolean[] possible = new boolean[target + 1];
    possible[0] = true;
    for (int value : values) {
      for (int sum = target; sum >= value; sum--) possible[sum] |= possible[sum - value];
    }
    System.out.println(possible[target] ? "YES" : "NO");
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
