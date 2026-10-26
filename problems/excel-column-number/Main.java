import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String text = input.readLine();
    long column = 0;
    for (int index = 0; index < text.length(); index++) column = column * 26 + text.charAt(index) - 'A' + 1;
    System.out.println(column);
  }
}
