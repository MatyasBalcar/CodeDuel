import java.util.*;

public class Solution {
  public boolean solve(int n, int[][] prerequisites) {
    List<List<Integer>> graph = new ArrayList<>();
    for (int course = 0; course < n; course++) graph.add(new ArrayList<>());
    int[] remaining = new int[n];
    for (int[] pair : prerequisites) {
      graph.get(pair[0] - 1).add(pair[1] - 1);
      remaining[pair[1] - 1]++;
    }
    int[] queue = new int[n];
    int head = 0;
    int tail = 0;
    for (int course = 0; course < n; course++) if (remaining[course] == 0) queue[tail++] = course;
    while (head < tail) {
      int course = queue[head++];
      for (int next : graph.get(course)) if (--remaining[next] == 0) queue[tail++] = next;
    }
    return tail == n;
  }
}
