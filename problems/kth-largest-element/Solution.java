import java.util.*;

public class Solution {
  public int solve(int[] nums, int k) {
    PriorityQueue<Integer> largest = new PriorityQueue<>();
    for (int value : nums) {
      largest.add(value);
      if (largest.size() > k) largest.poll();
    }
    return largest.peek();
  }
}
