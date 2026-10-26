import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    String text = input.readLine();
    int[] counts = new int[26];
    for (int index = 0; index < text.length(); index++) counts[text.charAt(index) - 'a']++;
    for (int index = 0; index < text.length(); index++) {
      if (counts[text.charAt(index) - 'a'] == 1) {
        System.out.println(index);
        return;
      }
    }
    System.out.println(-1);
  }
}
