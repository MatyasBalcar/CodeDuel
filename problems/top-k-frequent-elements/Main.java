import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    int limit = input.integer();
    Map<Integer, Integer> frequencies = new HashMap<>();
    for (int index = 0; index < count; index++) frequencies.merge(input.integer(), 1, Integer::sum);
    List<Integer> values = new ArrayList<>(frequencies.keySet());
    values.sort((first, second) -> {
      int frequencyOrder = Integer.compare(frequencies.get(second), frequencies.get(first));
      return frequencyOrder != 0 ? frequencyOrder : Integer.compare(first, second);
    });
    StringBuilder output = new StringBuilder();
    for (int index = 0; index < limit; index++) {
      if (index > 0) output.append(' ');
      output.append(values.get(index));
    }
    System.out.println(output);
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
