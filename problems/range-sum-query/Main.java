import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    int queries = input.integer();
    long[] prefix = new long[count + 1];
    for (int index = 1; index <= count; index++) prefix[index] = prefix[index - 1] + input.number();
    StringBuilder output = new StringBuilder();
    for (int query = 0; query < queries; query++) {
      int left = input.integer();
      int right = input.integer();
      output.append(prefix[right] - prefix[left - 1]).append('\n');
    }
    System.out.print(output);
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
