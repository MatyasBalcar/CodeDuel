import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int rows = input.integer(), cols = input.integer();
    char[][] grid = new char[rows][];
    for (int r = 0; r < rows; r++) grid[r] = input.next().toCharArray();
    if (grid[0][0] == '#' || grid[rows - 1][cols - 1] == '#') {
      System.out.println(-1);
      return;
    }
    int[] distance = new int[rows * cols];
    Arrays.fill(distance, -1);
    int[] queue = new int[rows * cols];
    int head = 0, tail = 1;
    distance[0] = 0;
    int[] dr = {-1, 1, 0, 0}, dc = {0, 0, -1, 1};
    while (head < tail) {
      int current = queue[head++], r = current / cols, c = current % cols;
      for (int direction = 0; direction < 4; direction++) {
        int nr = r + dr[direction], nc = c + dc[direction];
        if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || grid[nr][nc] == '#') continue;
        int next = nr * cols + nc;
        if (distance[next] == -1) {
          distance[next] = distance[current] + 1;
          queue[tail++] = next;
        }
      }
    }
    System.out.println(distance[rows * cols - 1]);
  }

  private static final class Input {
    private final byte[] buffer = new byte[65536];
    private int offset, size;

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
