import java.io.*;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    Input input = new Input();
    int count = input.integer();
    long firstBuy = Long.MIN_VALUE;
    long secondBuy = Long.MIN_VALUE;
    long firstSell = 0;
    long secondSell = 0;
    for (int day = 0; day < count; day++) {
      long price = input.number();
      firstBuy = Math.max(firstBuy, -price);
      firstSell = Math.max(firstSell, firstBuy + price);
      secondBuy = Math.max(secondBuy, firstSell - price);
      secondSell = Math.max(secondSell, secondBuy + price);
    }
    System.out.println(secondSell);
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
