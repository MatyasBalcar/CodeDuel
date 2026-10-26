import java.util.*;

public class Solution {
  public int[] solve(int[] nums, int k) {
    Map<Integer, Integer> frequencies = new HashMap<>();
    for (int value : nums) frequencies.merge(value, 1, Integer::sum);
    List<Integer> values = new ArrayList<>(frequencies.keySet());
    values.sort((first, second) -> {
      int frequencyOrder = Integer.compare(frequencies.get(second), frequencies.get(first));
      return frequencyOrder != 0 ? frequencyOrder : Integer.compare(first, second);
    });
    int[] result = new int[k];
    for (int index = 0; index < k; index++) result[index] = values.get(index);
    return result;
  }
}
