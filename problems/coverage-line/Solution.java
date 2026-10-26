import java.util.*;

public class Solution {
  public long solve(long[][] intervals) {
    Arrays.sort(intervals, Comparator.comparingLong(interval -> interval[0]));
    long total = 0;
    long end = Long.MIN_VALUE;
    for (long[] interval : intervals) {
      long start = Math.max(interval[0], end);
      if (interval[1] > start) total += interval[1] - start;
      end = Math.max(end, interval[1]);
    }
    return total;
  }
}
