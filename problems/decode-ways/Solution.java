import java.util.*;

public class Solution {
  public int solve(String digits) {
    long previous = digits.charAt(0) == '0' ? 0 : 1;
    long twoBack = 1;
    for (int index = 1; index < digits.length(); index++) {
      long current = digits.charAt(index) == '0' ? 0 : previous;
      int pair = (digits.charAt(index - 1) - '0') * 10 + digits.charAt(index) - '0';
      if (pair >= 10 && pair <= 26) current += twoBack;
      twoBack = previous;
      previous = current % 1000000007;
    }
    return (int) previous;
  }
}
