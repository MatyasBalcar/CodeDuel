import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String text = input.readLine();
    int end = text.length() - 1;
    while (end >= 0 && text.charAt(end) == ' ') end--;
    int start = end;
    while (start >= 0 && text.charAt(start) != ' ') start--;
    System.out.println(end - start);
  }
}
