import java.util.*;

public class Solution {
  public int solve(int[][] reservations) {
    Arrays.sort(reservations, Comparator.comparingInt(reservation -> reservation[0]));
    PriorityQueue<Integer> departures = new PriorityQueue<>();
    int slots = 0;
    for (int[] reservation : reservations) {
      while (!departures.isEmpty() && departures.peek() <= reservation[0]) departures.poll();
      departures.add(reservation[1]);
      slots = Math.max(slots, departures.size());
    }
    return slots;
  }
}
