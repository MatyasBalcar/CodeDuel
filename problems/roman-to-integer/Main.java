import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String text = input.readLine();
    Map<Character, Integer> values = Map.of('I', 1, 'V', 5, 'X', 10, 'L', 50, 'C', 100, 'D', 500, 'M', 1000);
    int total = 0;
    int previous = 0;
    for (int index = text.length() - 1; index >= 0; index--) {
      int value = values.get(text.charAt(index));
      total += value < previous ? -value : value;
      previous = value;
    }
    System.out.println(total);
  }
}
