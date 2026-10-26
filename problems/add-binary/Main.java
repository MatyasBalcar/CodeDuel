import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String first = input.readLine();
    String second = input.readLine();
    int left = first.length() - 1;
    int right = second.length() - 1;
    int carry = 0;
    StringBuilder answer = new StringBuilder();
    while (left >= 0 || right >= 0 || carry > 0) {
      int sum = carry;
      if (left >= 0) sum += first.charAt(left--) - '0';
      if (right >= 0) sum += second.charAt(right--) - '0';
      answer.append((char) ('0' + sum % 2));
      carry = sum / 2;
    }
    System.out.println(answer.reverse());
  }
}
