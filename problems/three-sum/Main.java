import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    int[] values = new int[count];
    for (int index = 0; index < count; index++) values[index] = input.integer();
    Arrays.sort(values);
    long answer = 0;
    for (int index = 0; index < count - 2; index++) {
      if (index > 0 && values[index] == values[index - 1]) continue;
      int left = index + 1;
      int right = count - 1;
      while (left < right) {
        long sum = (long) values[index] + values[left] + values[right];
        if (sum < 0) left++;
        else if (sum > 0) right--;
        else {
          answer++;
          int first = values[left];
          int last = values[right];
          while (left < right && values[left] == first) left++;
          while (left < right && values[right] == last) right--;
        }
      }
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
