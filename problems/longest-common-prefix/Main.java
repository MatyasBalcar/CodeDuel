import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    int count = Integer.parseInt(input.readLine().trim());
    if (count == 0) {
      System.out.println();
      return;
    }
    String first = input.readLine();
    int length = first.length();
    for (int index = 1; index < count; index++) {
      String word = input.readLine();
      length = Math.min(length, word.length());
      int matched = 0;
      while (matched < length && first.charAt(matched) == word.charAt(matched)) matched++;
      length = matched;
    }
    System.out.println(first.substring(0, length));
  }
}
