import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    int[] temperatures = new int[count];
    int[] waiting = new int[count];
    int[] stack = new int[count];
    int size = 0;
    for (int day = 0; day < count; day++) {
      temperatures[day] = input.integer();
      while (size > 0 && temperatures[stack[size - 1]] < temperatures[day]) {
        int previous = stack[--size];
        waiting[previous] = day - previous;
      }
      stack[size++] = day;
    }
    StringBuilder output = new StringBuilder();
    for (int day = 0; day < count; day++) {
      if (day > 0) output.append(' ');
      output.append(waiting[day]);
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
