import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws Exception {
        String signal = new BufferedReader(new InputStreamReader(System.in)).readLine();
        if (signal == null) signal = "";
        int[] last = new int[128];
        int left = 0;
        int best = 0;
        for (int right = 0; right < signal.length(); right++) {
            char symbol = signal.charAt(right);
            left = Math.max(left, last[symbol]);
            best = Math.max(best, right - left + 1);
            last[symbol] = right + 1;
        }
        System.out.println(best);
    }
}
