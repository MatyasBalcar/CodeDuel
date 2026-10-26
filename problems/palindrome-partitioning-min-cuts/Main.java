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
    boolean[][] palindrome = new boolean[count][count];
    for (int left = count - 1; left >= 0; left--) {
      for (int right = left; right < count; right++) {
        palindrome[left][right] = text.charAt(left) == text.charAt(right)
            && (right - left < 2 || palindrome[left + 1][right - 1]);
      }
    }
    int[] cuts = new int[count + 1];
    cuts[0] = -1;
    for (int end = 1; end <= count; end++) {
      cuts[end] = end - 1;
      for (int start = 0; start < end; start++) {
        if (palindrome[start][end - 1]) cuts[end] = Math.min(cuts[end], cuts[start] + 1);
      }
    }
    System.out.println(cuts[count]);
  }
}
