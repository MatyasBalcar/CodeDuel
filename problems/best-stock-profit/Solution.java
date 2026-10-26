import java.util.*;

public class Solution {
  public long solve(int[] prices) {
    long cheapest = Long.MAX_VALUE;
    long profit = 0;
    for (int price : prices) {
      cheapest = Math.min(cheapest, price);
      profit = Math.max(profit, price - cheapest);
    }
    return profit;
  }
}
