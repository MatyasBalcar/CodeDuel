import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String text = input.readLine();
    String pattern = input.readLine();
    boolean[][] matches = new boolean[text.length() + 1][pattern.length() + 1];
    matches[0][0] = true;
    for (int column = 2; column <= pattern.length(); column++) {
      if (pattern.charAt(column - 1) == '*') matches[0][column] = matches[0][column - 2];
    }
    for (int row = 1; row <= text.length(); row++) {
      for (int column = 1; column <= pattern.length(); column++) {
        char current = pattern.charAt(column - 1);
        if (current == '*') {
          char previous = pattern.charAt(column - 2);
          matches[row][column] = matches[row][column - 2]
              || ((previous == '.' || previous == text.charAt(row - 1)) && matches[row - 1][column]);
        } else {
          matches[row][column] = (current == '.' || current == text.charAt(row - 1))
              && matches[row - 1][column - 1];
        }
      }
    }
    System.out.println(matches[text.length()][pattern.length()] ? "YES" : "NO");
  }
}
