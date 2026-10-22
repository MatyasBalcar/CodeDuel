import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int n = input.integer(), target = input.integer();
    int[] coins = new int[n];
    for (int i = 0; i < n; i++) coins[i] = input.integer();
    int[] best = new int[target + 1];
    Arrays.fill(best, target + 1);
    best[0] = 0;
    for (int value = 1; value <= target; value++) {
      for (int coin : coins)
        if (coin <= value) best[value] = Math.min(best[value], best[value - coin] + 1);
    }
    System.out.println(best[target] > target ? -1 : best[target]);
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
