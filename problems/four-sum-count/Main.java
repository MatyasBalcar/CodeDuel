import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    long[][] values = new long[4][count];
    for (int group = 0; group < 4; group++) {
      for (int index = 0; index < count; index++) values[group][index] = input.number();
    }
    Map<Long, Integer> pairs = new HashMap<>();
    for (long first : values[0]) {
      for (long second : values[1]) pairs.merge(first + second, 1, Integer::sum);
    }
    long answer = 0;
    for (long third : values[2]) {
      for (long fourth : values[3]) answer += pairs.getOrDefault(-third - fourth, 0);
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
