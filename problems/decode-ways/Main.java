import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String text = input.readLine();
    long previous = text.charAt(0) == '0' ? 0 : 1;
    long twoBack = 1;
    for (int index = 1; index < text.length(); index++) {
      long current = text.charAt(index) == '0' ? 0 : previous;
      int pair = (text.charAt(index - 1) - '0') * 10 + text.charAt(index) - '0';
      if (pair >= 10 && pair <= 26) current += twoBack;
      twoBack = previous;
      previous = current % 1000000007;
    }
    System.out.println(previous);
  }
}
