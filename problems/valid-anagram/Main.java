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
    int[] counts = new int[26];
    for (int index = 0; index < first.length(); index++) {
      counts[first.charAt(index) - 'a']++;
      counts[second.charAt(index) - 'a']--;
    }
    for (int count : counts) {
      if (count != 0) {
        System.out.println("NO");
        return;
      }
    }
    System.out.println("YES");
  }
}
