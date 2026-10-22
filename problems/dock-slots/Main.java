import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int n = input.integer();
    int[][] visits = new int[n][2];
    for (int i = 0; i < n; i++) {
      visits[i][0] = input.integer();
      visits[i][1] = input.integer();
    }
    Arrays.sort(visits, Comparator.comparingInt(a -> a[0]));
    PriorityQueue<Integer> departures = new PriorityQueue<>();
    int best = 0;
    for (int[] visit : visits) {
      while (!departures.isEmpty() && departures.peek() <= visit[0]) departures.remove();
      departures.add(visit[1]);
      best = Math.max(best, departures.size());
    }
    System.out.println(best);
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
