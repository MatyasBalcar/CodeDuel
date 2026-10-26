import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int level = input.integer();
    long[] row = new long[level + 1];
    row[0] = 1;
    for (int current = 1; current <= level; current++) {
      for (int column = current; column > 0; column--) row[column] += row[column - 1];
    }
    StringBuilder output = new StringBuilder();
    for (int column = 0; column <= level; column++) {
      if (column > 0) output.append(' ');
      output.append(row[column]);
    }
    System.out.println(output);
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
