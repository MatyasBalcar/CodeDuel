import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String text = input.readLine();
    int left = 0;
    int right = text.length() - 1;
    while (left < right) {
      if (text.charAt(left) != text.charAt(right)) {
        System.out.println("NO");
        return;
      }
      left++;
      right--;
    }
    System.out.println("YES");
  }
}
