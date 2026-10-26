import java.util.*;

public class Solution {
  public long solve(String column) {
    long value = 0;
    for (int index = 0; index < column.length(); index++) value = value * 26 + column.charAt(index) - 'A' + 1;
    return value;
  }
}
