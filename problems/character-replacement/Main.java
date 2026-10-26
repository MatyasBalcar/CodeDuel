import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    int replacements = Integer.parseInt(input.readLine().trim());
    String text = input.readLine();
    int[] counts = new int[26];
    int left = 0;
    int best = 0;
    for (int right = 0; right < text.length(); right++) {
      counts[text.charAt(right) - 'A']++;
      while (right - left + 1 - largest(counts) > replacements) counts[text.charAt(left++) - 'A']--;
      best = Math.max(best, right - left + 1);
    }
    System.out.println(best);
  }

  private static int largest(int[] counts) {
    int largest = 0;
    for (int count : counts) largest = Math.max(largest, count);
    return largest;
  }
}
