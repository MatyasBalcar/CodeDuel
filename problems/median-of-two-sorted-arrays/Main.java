import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int firstCount = input.integer();
    int secondCount = input.integer();
    int[] first = new int[firstCount];
    int[] second = new int[secondCount];
    for (int index = 0; index < firstCount; index++) first[index] = input.integer();
    for (int index = 0; index < secondCount; index++) second[index] = input.integer();
    System.out.printf(Locale.ROOT, "%.1f%n", median(first, second));
  }

  private static double median(int[] first, int[] second) {
    if (first.length > second.length) return median(second, first);
    int half = (first.length + second.length + 1) / 2;
    int left = 0;
    int right = first.length;
    while (left <= right) {
      int splitFirst = left + (right - left) / 2;
      int splitSecond = half - splitFirst;
      long beforeFirst = splitFirst == 0 ? Long.MIN_VALUE : first[splitFirst - 1];
      long afterFirst = splitFirst == first.length ? Long.MAX_VALUE : first[splitFirst];
      long beforeSecond = splitSecond == 0 ? Long.MIN_VALUE : second[splitSecond - 1];
      long afterSecond = splitSecond == second.length ? Long.MAX_VALUE : second[splitSecond];
      if (beforeFirst <= afterSecond && beforeSecond <= afterFirst) {
        long lower = Math.max(beforeFirst, beforeSecond);
        if ((first.length + second.length) % 2 == 1) return lower;
        return (lower + Math.min(afterFirst, afterSecond)) / 2.0;
      }
      if (beforeFirst > afterSecond) right = splitFirst - 1;
      else left = splitFirst + 1;
    }
    throw new IllegalStateException("Arrays must be sorted");
  }

  private static final class Input {
    private final byte[] buffer = new byte[65536];
    private int offset;
    private int size;

    private int read() throws IOException {
      if (offset >= size) {
        size = System.in.read(buffer);
        offset = 0;
        if (size < 0) return -1;
      }
      return buffer[offset++];
    }

    String next() throws IOException {
      StringBuilder value = new StringBuilder();
      int c;
      do {
        c = read();
      } while (c >= 0 && c <= 32);
      while (c > 32) {
        value.append((char) c);
        c = read();
      }
      return value.toString();
    }

    int integer() throws IOException {
      return Integer.parseInt(next());
    }

    long number() throws IOException {
      return Long.parseLong(next());
    }
  }
}
