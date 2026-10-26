import java.util.*;

public class Solution {
  public boolean solve(int[] jumps) {
    long farthest = 0;
    for (int index = 0; index < jumps.length; index++) {
      if (index > farthest) return false;
      farthest = Math.max(farthest, index + (long) jumps[index]);
    }
    return farthest >= jumps.length - 1;
  }
}
