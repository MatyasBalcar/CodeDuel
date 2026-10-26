import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String first = input.readLine();
    String second = input.readLine();
    if (first.length() != second.length()) {
      System.out.println("NO");
      return;
    }
    int[] forward = new int[26];
    int[] backward = new int[26];
    Arrays.fill(forward, -1);
    Arrays.fill(backward, -1);
    for (int index = 0; index < first.length(); index++) {
      int from = first.charAt(index) - 'a';
      int to = second.charAt(index) - 'a';
      if ((forward[from] >= 0 && forward[from] != to) || (backward[to] >= 0 && backward[to] != from)) {
        System.out.println("NO");
        return;
      }
      forward[from] = to;
      backward[to] = from;
    }
    System.out.println("YES");
  }
}
