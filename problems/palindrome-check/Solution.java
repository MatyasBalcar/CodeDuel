import java.util.*;

public class Solution {
  public boolean solve(String text) {
    int left = 0;
    int right = text.length() - 1;
    while (left < right) {
      if (text.charAt(left++) != text.charAt(right--)) return false;
    }
    return true;
  }
}
