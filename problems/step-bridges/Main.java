import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int destination = input.integer(), blockedCount = input.integer();
    boolean[] blocked = new boolean[destination + 1];
    for (int i = 0; i < blockedCount; i++) blocked[input.integer()] = true;
    long previous = 1, beforePrevious = 0;
    for (int step = 1; step <= destination; step++) {
      long ways = blocked[step] ? 0 : (previous + beforePrevious) % 1000000007L;
      beforePrevious = previous;
      previous = ways;
    }
    System.out.println(previous);
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
