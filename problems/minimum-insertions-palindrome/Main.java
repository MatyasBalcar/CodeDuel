import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String text = input.readLine();
    int count = text.length();
    if (count == 0) {
      System.out.println(0);
      return;
    }
    int[][] best = new int[count][count];
    for (int length = 2; length <= count; length++) {
      for (int left = 0; left + length <= count; left++) {
        int right = left + length - 1;
        if (text.charAt(left) == text.charAt(right)) {
          best[left][right] = length == 2 ? 0 : best[left + 1][right - 1];
        } else {
          best[left][right] = 1 + Math.min(best[left + 1][right], best[left][right - 1]);
        }
      }
    }
    System.out.println(best[0][count - 1]);
  }
}
