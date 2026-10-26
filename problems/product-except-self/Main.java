import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    long[] values = new long[count];
    long[] answer = new long[count];
    long prefix = 1;
    for (int index = 0; index < count; index++) {
      values[index] = input.number();
      answer[index] = prefix;
      prefix *= values[index];
    }
    long suffix = 1;
    for (int index = count - 1; index >= 0; index--) {
      answer[index] *= suffix;
      suffix *= values[index];
    }
    StringBuilder output = new StringBuilder();
    for (int index = 0; index < count; index++) {
      if (index > 0) output.append(' ');
      output.append(answer[index]);
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
