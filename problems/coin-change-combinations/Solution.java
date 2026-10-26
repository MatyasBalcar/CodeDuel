import java.util.*;

public class Solution {
  public int solve(int[] coins, int target) {
    int[] ways = new int[target + 1];
    ways[0] = 1;
    for (int coin : coins) {
      for (int amount = coin; amount <= target; amount++) ways[amount] = (ways[amount] + ways[amount - coin]) % 1000000007;
    }
    return ways[target];
  }
}
