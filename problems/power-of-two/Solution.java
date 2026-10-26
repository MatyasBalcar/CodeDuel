import java.util.*;

public class Solution {
  public boolean solve(long n) {
    return n > 0 && (n & (n - 1)) == 0;
  }
}
