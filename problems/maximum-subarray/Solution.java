import java.util.*;

public class Solution {
  public long solve(int[] nums) {
    long endingHere = nums[0];
    long best = endingHere;
    for (int index = 1; index < nums.length; index++) {
      endingHere = Math.max(nums[index], endingHere + nums[index]);
      best = Math.max(best, endingHere);
    }
    return best;
  }
}
