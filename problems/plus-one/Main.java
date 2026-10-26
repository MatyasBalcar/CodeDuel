import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    int[] digits = new int[count + 1];
    for (int index = 1; index <= count; index++) digits[index] = input.integer();
    int carry = 1;
    for (int index = count; index >= 1 && carry > 0; index--) {
      int sum = digits[index] + carry;
      digits[index] = sum % 10;
      carry = sum / 10;
    }
    digits[0] = carry;
    int start = digits[0] == 0 ? 1 : 0;
    StringBuilder output = new StringBuilder();
    for (int index = start; index <= count; index++) {
      if (index > start) output.append(' ');
      output.append(digits[index]);
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
