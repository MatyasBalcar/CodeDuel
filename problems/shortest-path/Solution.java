import java.util.*;

public class Solution {
  public long solve(int n, int[][] edges, int source, int target) {
    List<List<Edge>> graph = new ArrayList<>();
    for (int vertex = 0; vertex < n; vertex++) graph.add(new ArrayList<>());
    for (int[] edge : edges) graph.get(edge[0] - 1).add(new Edge(edge[1] - 1, edge[2]));
    source--;
    target--;
    long[] distance = new long[n];
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
    return distance[target] == Long.MAX_VALUE ? -1 : distance[target];
  }

  private record Edge(int to, long weight) {}

  private record Route(int vertex, long cost) {}
}
