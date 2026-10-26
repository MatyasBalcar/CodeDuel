import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String text = input.readLine();
    int[] stack = new int[text.length() + 1];
    int top = 0;
    stack[0] = -1;
    int best = 0;
    for (int index = 0; index < text.length(); index++) {
      if (text.charAt(index) == '(') {
        stack[++top] = index;
      } else if (top == 0) {
        stack[0] = index;
      } else {
        top--;
        best = Math.max(best, index - stack[top]);
      }
    }
    System.out.println(best);
  }
}
