import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String first = input.readLine();
    String second = input.readLine();
    int[] previous = new int[second.length() + 1];
    int[] current = new int[second.length() + 1];
    for (int column = 0; column <= second.length(); column++) previous[column] = column;
    for (int row = 1; row <= first.length(); row++) {
      current[0] = row;
      for (int column = 1; column <= second.length(); column++) {
        if (first.charAt(row - 1) == second.charAt(column - 1)) {
          current[column] = previous[column - 1];
        } else {
          current[column] = 1 + Math.min(previous[column - 1], Math.min(previous[column], current[column - 1]));
        }
      }
      int[] swap = previous;
      previous = current;
      current = swap;
    }
    System.out.println(previous[second.length()]);
  }
}
