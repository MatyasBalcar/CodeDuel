import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String path = input.readLine();
    Deque<String> directories = new ArrayDeque<>();
    for (String part : path.split("/")) {
      if (part.isEmpty() || part.equals(".")) continue;
      if (part.equals("..")) {
        if (!directories.isEmpty()) directories.removeLast();
      } else {
        directories.addLast(part);
      }
    }
    System.out.println("/" + String.join("/", directories));
  }
}
