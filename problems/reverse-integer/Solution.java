import java.util.*;

public class Solution {
  public int solve(int n) {
    long value = n;
    long reversed = 0;
    while (value != 0) {
      reversed = reversed * 10 + value % 10;
      value /= 10;
    }
    return reversed < Integer.MIN_VALUE || reversed > Integer.MAX_VALUE ? 0 : (int) reversed;
  }
}
