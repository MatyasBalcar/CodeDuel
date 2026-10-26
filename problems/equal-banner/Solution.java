import java.util.*;

public class Solution {
  public int solve(int[] tiles) {
    Map<Integer, Integer> earliest = new HashMap<>();
    earliest.put(0, -1);
    int balance = 0;
    int best = 0;
    for (int index = 0; index < tiles.length; index++) {
      balance += tiles[index] == 0 ? -1 : 1;
      Integer previous = earliest.get(balance);
      if (previous == null) earliest.put(balance, index);
      else best = Math.max(best, index - previous);
    }
    return best;
  }
}
