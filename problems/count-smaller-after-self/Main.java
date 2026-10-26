import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    int[] values = new int[count];
    for (int index = 0; index < count; index++) values[index] = input.integer();
    int[] ordered = values.clone();
    Arrays.sort(ordered);
    int[] tree = new int[count + 1];
    int[] answer = new int[count];
    for (int index = count - 1; index >= 0; index--) {
      int rank = lowerBound(ordered, values[index]) + 1;
      for (int position = rank - 1; position > 0; position -= position & -position) answer[index] += tree[position];
      for (int position = rank; position < tree.length; position += position & -position) tree[position]++;
    }
    StringBuilder output = new StringBuilder();
    for (int index = 0; index < count; index++) {
      if (index > 0) output.append(' ');
      output.append(answer[index]);
    }
    System.out.println(output);
  }

  private static int lowerBound(int[] values, int target) {
    int left = 0;
    int right = values.length;
    while (left < right) {
      int middle = left + (right - left) / 2;
      if (values[middle] < target) left = middle + 1;
      else right = middle;
    }
    return left;
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
