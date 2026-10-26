import java.util.*;

public class Solution {
  public long solve(long n) {
    long zeroes = 0;
    for (long remaining = n / 5; remaining > 0; remaining /= 5) zeroes += remaining;
    return zeroes;
  }
}
