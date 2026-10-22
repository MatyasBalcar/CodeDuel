import java.io.BufferedInputStream;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final BufferedInputStream INPUT = new BufferedInputStream(System.in);

    private static long nextLong() throws Exception {
        int c;
        do { c = INPUT.read(); } while (c <= ' ' && c != -1);
        long sign = 1;
        if (c == '-') { sign = -1; c = INPUT.read(); }
        long value = 0;
        while (c > ' ') {
            value = value * 10 + c - '0';
            c = INPUT.read();
        }
        return value * sign;
    }

    public static void main(String[] args) throws Exception {
        int count = (int) nextLong();
        long target = nextLong();
        Map<Long, Long> seen = new HashMap<>();
        long pairs = 0;
        for (int i = 0; i < count; i++) {
            long weight = nextLong();
            pairs += seen.getOrDefault(target - weight, 0L);
            seen.merge(weight, 1L, Long::sum);
        }
        System.out.println(pairs);
    }
}
