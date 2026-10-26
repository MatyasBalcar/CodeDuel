import java.util.*;

public class Solution {
  public long solve(int[] prices) {
    long firstBuy = Long.MIN_VALUE;
    long firstSell = 0;
    long secondBuy = Long.MIN_VALUE;
    long secondSell = 0;
    for (int price : prices) {
      firstBuy = Math.max(firstBuy, -(long) price);
      firstSell = Math.max(firstSell, firstBuy + price);
      secondBuy = Math.max(secondBuy, firstSell - price);
      secondSell = Math.max(secondSell, secondBuy + price);
    }
    return secondSell;
  }
}
