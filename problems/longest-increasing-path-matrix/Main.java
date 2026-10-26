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
    int[] dr = {-1, 1, 0, 0};
    int[] dc = {0, 0, -1, 1};
    int[][] greater = new int[rows][columns];
    int[] queue = new int[rows * columns];
    int head = 0;
    int tail = 0;
    for (int row = 0; row < rows; row++) {
      for (int column = 0; column < columns; column++) {
        for (int direction = 0; direction < 4; direction++) {
          int nr = row + dr[direction];
          int nc = column + dc[direction];
          if (nr >= 0 && nr < rows && nc >= 0 && nc < columns && matrix[nr][nc] > matrix[row][column]) {
            greater[row][column]++;
          }
        }
        if (greater[row][column] == 0) queue[tail++] = row * columns + column;
      }
    }
    int length = 0;
    while (head < tail) {
      int layerEnd = tail;
      length++;
      while (head < layerEnd) {
        int current = queue[head++];
        int row = current / columns;
        int column = current % columns;
        for (int direction = 0; direction < 4; direction++) {
          int nr = row + dr[direction];
          int nc = column + dc[direction];
          if (nr >= 0 && nr < rows && nc >= 0 && nc < columns && matrix[nr][nc] < matrix[row][column]) {
            if (--greater[nr][nc] == 0) queue[tail++] = nr * columns + nc;
          }
        }
      }
    }
    System.out.println(length);
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
