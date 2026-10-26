import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    long target = input.number();
    Map<Long, Integer> firstPosition = new HashMap<>();
    int bestFirst = -1;
    int bestSecond = -1;
    for (int index = 0; index < count; index++) {
      long value = input.number();
      Integer first = firstPosition.get(target - value);
      if (first != null && (bestFirst == -1 || first < bestFirst)) {
        bestFirst = first;
        bestSecond = index;
      }
      firstPosition.putIfAbsent(value, index);
    }
    System.out.println(bestFirst == -1 ? "-1" : (bestFirst + 1) + " " + (bestSecond + 1));
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
