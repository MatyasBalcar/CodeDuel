import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws Exception {
        String signal = new BufferedReader(new InputStreamReader(System.in)).readLine();
        if (signal == null) signal = "";
        char[] stack = new char[signal.length()];
        int size = 0;
        for (char bracket : signal.toCharArray()) {
            if (bracket == '(' || bracket == '[' || bracket == '{') {
                stack[size++] = bracket;
            } else {
                char expected = bracket == ')' ? '(' : bracket == ']' ? '[' : '{';
                if (size == 0 || stack[--size] != expected) {
                    System.out.println("BROKEN");
                    return;
                }
            }
        }
        System.out.println(size == 0 ? "BALANCED" : "BROKEN");
    }
}
