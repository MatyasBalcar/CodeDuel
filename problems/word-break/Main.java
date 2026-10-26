import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String text = input.readLine();
    int count = Integer.parseInt(input.readLine().trim());
    Set<String> words = new HashSet<>();
    int longest = 0;
    for (int index = 0; index < count; index++) {
      String word = input.readLine();
      words.add(word);
      longest = Math.max(longest, word.length());
    }
    boolean[] possible = new boolean[text.length() + 1];
    possible[0] = true;
    for (int end = 1; end <= text.length(); end++) {
      for (int length = 1; length <= longest && length <= end; length++) {
        if (possible[end - length] && words.contains(text.substring(end - length, end))) {
          possible[end] = true;
          break;
        }
      }
    }
    System.out.println(possible[text.length()] ? "YES" : "NO");
  }
}
