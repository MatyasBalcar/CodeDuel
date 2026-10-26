import java.util.*;

public class Solution {
  public int solve(int n, int[] broken) {
    boolean[] blocked = new boolean[n + 1];
    for (int stone : broken) blocked[stone] = true;
    int[] ways = new int[n + 1];
    ways[0] = 1;
    for (int stone = 1; stone <= n; stone++) {
      if (blocked[stone]) continue;
      ways[stone] = ways[stone - 1];
      if (stone > 1) ways[stone] = (ways[stone] + ways[stone - 2]) % 1000000007;
    }
    return ways[n];
  }
}
