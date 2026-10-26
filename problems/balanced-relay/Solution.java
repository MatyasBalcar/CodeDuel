import java.util.*;

public class Solution {
  public String solve(String signal) {
    Deque<Character> stack = new ArrayDeque<>();
    for (int index = 0; index < signal.length(); index++) {
      char value = signal.charAt(index);
      if (value == '(' || value == '[' || value == '{') stack.push(value);
      else {
        if (stack.isEmpty()) return "BROKEN";
        char opening = stack.pop();
        if ((value == ')' && opening != '(') || (value == ']' && opening != '[')
            || (value == '}' && opening != '{')) return "BROKEN";
      }
    }
    return stack.isEmpty() ? "BALANCED" : "BROKEN";
  }
}
