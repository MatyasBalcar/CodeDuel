import java.util.*;

public class Solution {
  public long solve(int[] amounts) {
    long previous = 0;
    long twoBack = 0;
    for (int amount : amounts) {
      long best = Math.max(previous, twoBack + amount);
      twoBack = previous;
      previous = best;
    }
    return previous;
  }
}
