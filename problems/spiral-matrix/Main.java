import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int rows = input.integer();
    int columns = input.integer();
    int[][] matrix = new int[rows][columns];
    for (int row = 0; row < rows; row++) {
      for (int column = 0; column < columns; column++) matrix[row][column] = input.integer();
    }
    int top = 0;
    int bottom = rows - 1;
    int left = 0;
    int right = columns - 1;
    StringBuilder output = new StringBuilder();
    while (top <= bottom && left <= right) {
      for (int column = left; column <= right; column++) append(output, matrix[top][column]);
      top++;
      for (int row = top; row <= bottom; row++) append(output, matrix[row][right]);
      right--;
      if (top <= bottom) {
        for (int column = right; column >= left; column--) append(output, matrix[bottom][column]);
        bottom--;
      }
      if (left <= right) {
        for (int row = bottom; row >= top; row--) append(output, matrix[row][left]);
        left++;
      }
    }
    System.out.println(output);
  }

  private static void append(StringBuilder output, int value) {
    if (output.length() > 0) output.append(' ');
    output.append(value);
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
