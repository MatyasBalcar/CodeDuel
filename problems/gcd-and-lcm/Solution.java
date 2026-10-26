import java.util.*;

public class Solution {
  public long[] solve(long a, long b) {
    long first = a;
    long second = b;
    while (second != 0) {
      long remainder = first % second;
      first = second;
      second = remainder;
    }
    return new long[] {first, a / first * b};
  }
}
