import java.util.*;

public class Solution {
  public int[] solve(int[] temperatures) {
    int[] waiting = new int[temperatures.length];
    int[] stack = new int[temperatures.length];
    int size = 0;
    for (int day = 0; day < temperatures.length; day++) {
      while (size > 0 && temperatures[stack[size - 1]] < temperatures[day]) {
        int previous = stack[--size];
        waiting[previous] = day - previous;
      }
      stack[size++] = day;
    }
    return waiting;
  }
}
