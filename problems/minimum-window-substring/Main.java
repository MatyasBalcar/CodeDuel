import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String text = input.readLine();
    String target = input.readLine();
    if (target.isEmpty()) {
      System.out.println();
      return;
    }
    int[] needed = new int[128];
    for (int index = 0; index < target.length(); index++) needed[target.charAt(index)]++;
    int missing = target.length();
    int left = 0;
    int bestStart = 0;
    int bestLength = Integer.MAX_VALUE;
    for (int right = 0; right < text.length(); right++) {
      if (needed[text.charAt(right)]-- > 0) missing--;
      while (missing == 0) {
        if (right - left + 1 < bestLength) {
          bestStart = left;
          bestLength = right - left + 1;
        }
        if (++needed[text.charAt(left++)] > 0) missing++;
      }
    }
    System.out.println(bestLength == Integer.MAX_VALUE ? "NONE" : text.substring(bestStart, bestStart + bestLength));
  }
}
