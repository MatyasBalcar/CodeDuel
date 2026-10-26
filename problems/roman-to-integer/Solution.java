import java.util.*;

public class Solution {
  public int solve(String numeral) {
    Map<Character, Integer> values = Map.of('I', 1, 'V', 5, 'X', 10, 'L', 50, 'C', 100, 'D', 500, 'M', 1000);
    int total = 0;
    int previous = 0;
    for (int index = numeral.length() - 1; index >= 0; index--) {
      int value = values.get(numeral.charAt(index));
      total += value < previous ? -value : value;
      previous = value;
    }
    return total;
  }
}
