import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    int edgeCount = input.integer();
    List<List<Integer>> graph = new ArrayList<>();
    for (int course = 0; course < count; course++) graph.add(new ArrayList<>());
    int[] prerequisites = new int[count];
    for (int index = 0; index < edgeCount; index++) {
      int first = input.integer() - 1;
      int second = input.integer() - 1;
      graph.get(first).add(second);
      prerequisites[second]++;
    }
    int[] queue = new int[count];
    int head = 0;
    int tail = 0;
    for (int course = 0; course < count; course++) {
      if (prerequisites[course] == 0) queue[tail++] = course;
    }
    while (head < tail) {
      int course = queue[head++];
      for (int next : graph.get(course)) {
        if (--prerequisites[next] == 0) queue[tail++] = next;
      }
    }
    System.out.println(tail == count ? "YES" : "NO");
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
