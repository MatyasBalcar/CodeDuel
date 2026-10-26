import java.util.*;

public class Solution {
  public long solve(long n) {
    long left = 0;
    long right = Math.min(n, 1000000000L);
    long answer = 0;
    while (left <= right) {
      long middle = left + (right - left) / 2;
      if (middle == 0 || middle <= n / middle) {
        answer = middle;
        left = middle + 1;
      } else right = middle - 1;
    }
    return answer;
  }
}
