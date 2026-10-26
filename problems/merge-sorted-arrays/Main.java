import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int firstCount = input.integer();
    int secondCount = input.integer();
    int[] first = new int[firstCount];
    int[] second = new int[secondCount];
    for (int index = 0; index < firstCount; index++) first[index] = input.integer();
    for (int index = 0; index < secondCount; index++) second[index] = input.integer();
    int left = 0;
    int right = 0;
    StringBuilder output = new StringBuilder();
    while (left < firstCount || right < secondCount) {
      int value;
      if (right == secondCount || (left < firstCount && first[left] <= second[right])) {
        value = first[left++];
      } else {
        value = second[right++];
      }
      if (output.length() > 0) output.append(' ');
      output.append(value);
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
