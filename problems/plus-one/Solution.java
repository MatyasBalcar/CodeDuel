import java.util.*;

public class Solution {
  public int[] solve(int[] digits) {
    int[] result = Arrays.copyOf(digits, digits.length);
    for (int index = result.length - 1; index >= 0; index--) {
      if (result[index] < 9) {
        result[index]++;
        return result;
      }
      result[index] = 0;
    }
    result = new int[digits.length + 1];
    result[0] = 1;
    return result;
  }
}
