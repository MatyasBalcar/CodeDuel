import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String first = input.readLine();
    String second = input.readLine();
    int matched = 0;
    for (int index = 0; index < second.length(); index++) {
      if (matched < first.length() && first.charAt(matched) == second.charAt(index)) matched++;
    }
    System.out.println(matched == first.length() ? "YES" : "NO");
  }
}
