import java.util.*;

public class Solution {
  public int solve(int[][] intervals) {
    Arrays.sort(intervals, Comparator.comparingInt(interval -> interval[1]));
    int kept = 0;
    int end = Integer.MIN_VALUE;
    for (int[] interval : intervals) {
      if (interval[0] >= end) {
        kept++;
        end = interval[1];
      }
    }
    return intervals.length - kept;
  }
}
