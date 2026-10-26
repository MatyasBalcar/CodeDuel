import java.util.*;

public class Solution {
  public long[] solve(int[] nums, int[][] queries) {
    long[] prefix = new long[nums.length + 1];
    for (int index = 0; index < nums.length; index++) prefix[index + 1] = prefix[index] + nums[index];
    long[] answers = new long[queries.length];
    for (int index = 0; index < queries.length; index++) {
      answers[index] = prefix[queries[index][1]] - prefix[queries[index][0] - 1];
    }
    return answers;
  }
}
