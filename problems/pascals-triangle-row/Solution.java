import java.util.*;

public class Solution {
  public long[] solve(int k) {
    long[] result = new long[k + 1];
    result[0] = 1;
    for (int row = 1; row <= k; row++) {
      for (int column = row; column > 0; column--) result[column] += result[column - 1];
    }
    return result;
  }
}
