import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int rows = input.integer(), cols = input.integer();
    char[][] grid = new char[rows][];
    for (int r = 0; r < rows; r++) grid[r] = input.next().toCharArray();
    int[] parent = new int[rows * cols], size = new int[rows * cols];
    Arrays.fill(parent, -1);
    int groups = 0, largest = 0;
    for (int r = 0; r < rows; r++)
      for (int c = 0; c < cols; c++)
        if (grid[r][c] == '1') {
          int id = r * cols + c;
          parent[id] = id;
          size[id] = 1;
          groups++;
          largest = Math.max(largest, 1);
          int[] previous = {r > 0 ? id - cols : -1, c > 0 ? id - 1 : -1};
          for (int adjacent : previous)
            if (adjacent >= 0 && parent[adjacent] != -1) {
              int a = find(parent, id), b = find(parent, adjacent);
              if (a != b) {
                parent[b] = a;
                size[a] += size[b];
                largest = Math.max(largest, size[a]);
                groups--;
              }
            }
        }
    System.out.println(groups + " " + largest);
  }

  private static int find(int[] parent, int node) {
    while (parent[node] != node) {
      parent[node] = parent[parent[node]];
      node = parent[node];
    }
    return node;
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
