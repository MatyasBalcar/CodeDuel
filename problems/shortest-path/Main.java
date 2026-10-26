import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    int edgeCount = input.integer();
    int source = input.integer() - 1;
    int target = input.integer() - 1;
    List<List<Edge>> graph = new ArrayList<>();
    for (int vertex = 0; vertex < count; vertex++) graph.add(new ArrayList<>());
    for (int index = 0; index < edgeCount; index++) {
      int from = input.integer() - 1;
      int to = input.integer() - 1;
      graph.get(from).add(new Edge(to, input.number()));
    }
    long[] distance = new long[count];
    Arrays.fill(distance, Long.MAX_VALUE);
    distance[source] = 0;
    PriorityQueue<Route> pending = new PriorityQueue<>(Comparator.comparingLong(Route::cost));
    pending.add(new Route(source, 0));
    while (!pending.isEmpty()) {
      Route route = pending.poll();
      if (route.cost() != distance[route.vertex()]) continue;
      if (route.vertex() == target) break;
      for (Edge edge : graph.get(route.vertex())) {
        long cost = route.cost() + edge.weight();
        if (cost < distance[edge.to()]) {
          distance[edge.to()] = cost;
          pending.add(new Route(edge.to(), cost));
        }
      }
    }
    System.out.println(distance[target] == Long.MAX_VALUE ? -1 : distance[target]);
  }

  private record Edge(int to, long weight) {}

  private record Route(int vertex, long cost) {}

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
