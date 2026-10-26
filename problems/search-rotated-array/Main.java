import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    int target = input.integer();
    int[] values = new int[count];
    for (int index = 0; index < count; index++) values[index] = input.integer();
    int left = 0;
    int right = count - 1;
    int answer = -1;
    while (left <= right) {
      int middle = left + (right - left) / 2;
      if (values[middle] == target) {
        answer = middle;
        break;
      }
      if (values[left] <= values[middle]) {
        if (values[left] <= target && target < values[middle]) right = middle - 1;
        else left = middle + 1;
      } else {
        if (values[middle] < target && target <= values[right]) left = middle + 1;
        else right = middle - 1;
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
