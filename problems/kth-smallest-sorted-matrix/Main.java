import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    int rank = input.integer();
    int[][] matrix = new int[count][count];
    for (int row = 0; row < count; row++) {
      for (int column = 0; column < count; column++) matrix[row][column] = input.integer();
    }
    long left = matrix[0][0];
    long right = matrix[count - 1][count - 1];
    while (left < right) {
      long middle = left + (right - left) / 2;
      int smaller = 0;
      int column = count - 1;
      for (int row = 0; row < count; row++) {
        while (column >= 0 && matrix[row][column] > middle) column--;
        smaller += column + 1;
      }
      if (smaller < rank) left = middle + 1;
      else right = middle;
    }
    System.out.println(left);
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
