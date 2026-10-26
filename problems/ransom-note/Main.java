import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String note = input.readLine();
    String letters = input.readLine();
    int[] available = new int[26];
    for (int index = 0; index < letters.length(); index++) available[letters.charAt(index) - 'a']++;
    for (int index = 0; index < note.length(); index++) {
      if (--available[note.charAt(index) - 'a'] < 0) {
        System.out.println("NO");
        return;
      }
    }
    System.out.println("YES");
  }
}
