import java.util.*;

public class Solution {
  public String solve(String first, String second) {
    int left = first.length() - 1;
    int right = second.length() - 1;
    int carry = 0;
    StringBuilder result = new StringBuilder();
    while (left >= 0 || right >= 0 || carry > 0) {
      int sum = carry;
      if (left >= 0) sum += first.charAt(left--) - '0';
      if (right >= 0) sum += second.charAt(right--) - '0';
      result.append((char) ('0' + sum % 2));
      carry = sum / 2;
    }
    return result.reverse().toString();
  }
}
