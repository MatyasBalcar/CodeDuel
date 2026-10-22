import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int n = input.integer(), balance = 0, best = 0;
    Map<Integer, Integer> first = new HashMap<>();
    first.put(0, -1);
    for (int i = 0; i < n; i++) {
      balance += input.integer() == 1 ? 1 : -1;
      Integer previous = first.putIfAbsent(balance, i);
      if (previous != null) best = Math.max(best, i - previous);
    }
    System.out.println(best);
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
