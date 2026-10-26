import java.util.*;

public class Solution {
  public int solve(int[] coins, int target) {
    int[] best = new int[target + 1];
    Arrays.fill(best, target + 1);
    best[0] = 0;
    for (int value = 1; value <= target; value++) {
      for (int coin : coins) {
        if (coin <= value) best[value] = Math.min(best[value], best[value - coin] + 1);
      }
    }
    return best[target] > target ? -1 : best[target];
  }
}
