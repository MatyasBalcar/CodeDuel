import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String text = input.readLine();
    int best = 0;
    for (int center = 0; center < text.length(); center++) {
      best = Math.max(best, expand(text, center, center));
      best = Math.max(best, expand(text, center, center + 1));
    }
    System.out.println(best);
  }

  private static int expand(String text, int left, int right) {
    while (left >= 0 && right < text.length() && text.charAt(left) == text.charAt(right)) {
      left--;
      right++;
    }
    return right - left - 1;
  }
}
