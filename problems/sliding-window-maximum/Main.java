import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    int window = input.integer();
    int[] values = new int[count];
    int[] queue = new int[count];
    int head = 0;
    int tail = 0;
    StringBuilder output = new StringBuilder();
    for (int index = 0; index < count; index++) {
      values[index] = input.integer();
      while (head < tail && queue[head] <= index - window) head++;
      while (head < tail && values[queue[tail - 1]] <= values[index]) tail--;
      queue[tail++] = index;
      if (index >= window - 1) {
        if (output.length() > 0) output.append(' ');
        output.append(values[queue[head]]);
      }
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
