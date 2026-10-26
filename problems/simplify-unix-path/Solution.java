import java.util.*;

public class Solution {
  public String solve(String path) {
    Deque<String> directories = new ArrayDeque<>();
    for (String part : path.split("/")) {
      if (part.isEmpty() || part.equals(".")) continue;
      if (part.equals("..")) {
        if (!directories.isEmpty()) directories.removeLast();
      } else directories.addLast(part);
    }
    return "/" + String.join("/", directories);
  }
}
