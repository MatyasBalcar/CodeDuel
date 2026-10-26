import java.util.*;

public class Solution {
  public long solve(int[] a, int[] b, int[] c, int[] d) {
    Map<Long, Integer> pairs = new HashMap<>();
    for (int first : a) for (int second : b) pairs.merge((long) first + second, 1, Integer::sum);
    long count = 0;
    for (int third : c) for (int fourth : d) count += pairs.getOrDefault(-(long) third - fourth, 0);
    return count;
  }
}
