import java.util.*;

public class Solution {
  public long solve(String tasks, long cooldown) {
    if (tasks.isEmpty()) return 0;
    int[] counts = new int[26];
    for (int index = 0; index < tasks.length(); index++) counts[tasks.charAt(index) - 'A']++;
    int most = 0;
    int tied = 0;
    for (int count : counts) {
      if (count > most) { most = count; tied = 1; }
      else if (count == most) tied++;
    }
    return Math.max(tasks.length(), (most - 1L) * (cooldown + 1) + tied);
  }
}
