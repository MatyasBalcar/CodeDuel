import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    int edgeCount = input.integer();
    Edge[] edges = new Edge[edgeCount];
    for (int index = 0; index < edgeCount; index++) {
      edges[index] = new Edge(input.integer() - 1, input.integer() - 1, input.number());
    }
    Arrays.sort(edges, Comparator.comparingLong(Edge::weight));
    int[] parent = new int[count];
    int[] size = new int[count];
    for (int vertex = 0; vertex < count; vertex++) {
      parent[vertex] = vertex;
      size[vertex] = 1;
    }
    long total = 0;
    int chosen = 0;
    for (Edge edge : edges) {
      int first = find(parent, edge.first());
      int second = find(parent, edge.second());
      if (first == second) continue;
      if (size[first] < size[second]) {
        int swap = first;
        first = second;
        second = swap;
      }
      parent[second] = first;
      size[first] += size[second];
      total += edge.weight();
      chosen++;
      if (chosen == count - 1) break;
    }
    System.out.println(chosen == count - 1 ? Long.toString(total) : "IMPOSSIBLE");
  }

  private record Edge(int first, int second, long weight) {}

  private static int find(int[] parent, int vertex) {
    while (parent[vertex] != vertex) {
      parent[vertex] = parent[parent[vertex]];
      vertex = parent[vertex];
    }
    return vertex;
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
