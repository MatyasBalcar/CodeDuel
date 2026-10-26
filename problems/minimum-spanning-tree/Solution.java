import java.util.*;

public class Solution {
  public String solve(int n, int[][] edges) {
    Arrays.sort(edges, Comparator.comparingInt(edge -> edge[2]));
    int[] parent = new int[n];
    int[] size = new int[n];
    for (int vertex = 0; vertex < n; vertex++) {
      parent[vertex] = vertex;
      size[vertex] = 1;
    }
    long total = 0;
    int chosen = 0;
    for (int[] edge : edges) {
      int first = find(parent, edge[0] - 1);
      int second = find(parent, edge[1] - 1);
      if (first == second) continue;
      if (size[first] < size[second]) {
        int swap = first;
        first = second;
        second = swap;
      }
      parent[second] = first;
      size[first] += size[second];
      total += edge[2];
      if (++chosen == n - 1) break;
    }
    return chosen == n - 1 ? Long.toString(total) : "IMPOSSIBLE";
  }

  private static int find(int[] parent, int vertex) {
    while (parent[vertex] != vertex) {
      parent[vertex] = parent[parent[vertex]];
      vertex = parent[vertex];
    }
    return vertex;
  }
}
