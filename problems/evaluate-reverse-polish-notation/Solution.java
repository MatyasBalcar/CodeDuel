import java.util.*;

public class Solution {
  public long solve(String[] tokens) {
    long[] stack = new long[tokens.length];
    int size = 0;
    for (String token : tokens) {
      switch (token) {
        case "+" -> { long second = stack[--size]; stack[size - 1] += second; }
        case "-" -> { long second = stack[--size]; stack[size - 1] -= second; }
        case "*" -> { long second = stack[--size]; stack[size - 1] *= second; }
        case "/" -> { long second = stack[--size]; stack[size - 1] /= second; }
        default -> stack[size++] = Long.parseLong(token);
      }
    }
    return stack[0];
  }
}
