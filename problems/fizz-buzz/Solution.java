import java.util.*;

public class Solution {
  public String[] solve(int n) {
    String[] result = new String[n];
    for (int value = 1; value <= n; value++) {
      if (value % 15 == 0) result[value - 1] = "FizzBuzz";
      else if (value % 3 == 0) result[value - 1] = "Fizz";
      else if (value % 5 == 0) result[value - 1] = "Buzz";
      else result[value - 1] = Integer.toString(value);
    }
    return result;
  }
}
