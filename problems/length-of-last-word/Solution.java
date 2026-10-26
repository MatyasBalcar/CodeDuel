import java.util.*;

public class Solution {
  public int solve(String text) {
    int end = text.length() - 1;
    while (end >= 0 && text.charAt(end) == ' ') end--;
    int start = end;
    while (start >= 0 && text.charAt(start) != ' ') start--;
    return end - start;
  }
}
