import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {
    String input = new String(System.in.readAllBytes(), StandardCharsets.UTF_8);
    Object parsed = new Parser(input).parse();
    if (!(parsed instanceof Map<?, ?> arguments)) throw new IllegalArgumentException("Expected named arguments");
    PrintStream output = System.out;
    __RETURN_TYPE__ result;
    try {
      System.setOut(new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8));
      result = new Solution().solve(__ARGUMENTS__);
    } finally {
      System.setOut(output);
    }
    output.print(encode(result));
  }

  private static Object convert(Object value, String type) {
    if (type.endsWith("[]")) {
      List<?> values = (List<?>) value;
      String elementType = type.substring(0, type.length() - 2);
      Object array = Array.newInstance(javaType(elementType), values.size());
      for (int index = 0; index < values.size(); index++) Array.set(array, index, convert(values.get(index), elementType));
      return array;
    }
    return switch (type) {
      case "int" -> Math.toIntExact(((Number) value).longValue());
      case "long" -> ((Number) value).longValue();
      case "double" -> ((Number) value).doubleValue();
      case "boolean" -> (Boolean) value;
      case "String" -> (String) value;
      default -> throw new IllegalArgumentException("Unsupported argument type");
    };
  }

  private static Class<?> javaType(String type) {
    if (type.endsWith("[]")) return Array.newInstance(javaType(type.substring(0, type.length() - 2)), 0).getClass();
    return switch (type) {
      case "int" -> int.class;
      case "long" -> long.class;
      case "double" -> double.class;
      case "boolean" -> boolean.class;
      case "String" -> String.class;
      default -> throw new IllegalArgumentException("Unsupported array type");
    };
  }

  private static String encode(Object value) {
    if (value == null) return "null";
    if (value instanceof String text) {
      StringBuilder escaped = new StringBuilder("\"");
      for (int index = 0; index < text.length(); index++) {
        char current = text.charAt(index);
        switch (current) {
          case '"' -> escaped.append("\\\"");
          case '\\' -> escaped.append("\\\\");
          case '\n' -> escaped.append("\\n");
          case '\r' -> escaped.append("\\r");
          case '\t' -> escaped.append("\\t");
          default -> {
            if (current < 32) escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) current));
            else escaped.append(current);
          }
        }
      }
      return escaped.append('"').toString();
    }
    if (value.getClass().isArray()) {
      StringJoiner output = new StringJoiner(",", "[", "]");
      for (int index = 0; index < Array.getLength(value); index++) output.add(encode(Array.get(value, index)));
      return output.toString();
    }
    if (value instanceof Double number && !Double.isFinite(number)) throw new IllegalArgumentException("Return a finite number");
    if (value instanceof Number || value instanceof Boolean) return value.toString();
    throw new IllegalArgumentException("Unsupported return value");
  }

  private static final class Parser {
    private final String text;
    private int position;

    Parser(String text) {
      this.text = text;
    }

    Object parse() {
      Object value = value(0);
      whitespace();
      if (position != text.length()) throw new IllegalArgumentException("Trailing JSON data");
      return value;
    }

    private Object value(int depth) {
      if (depth > 16) throw new IllegalArgumentException("JSON is too deeply nested");
      whitespace();
      if (position == text.length()) throw new IllegalArgumentException("Missing JSON value");
      return switch (text.charAt(position)) {
        case '{' -> object(depth + 1);
        case '[' -> array(depth + 1);
        case '"' -> string();
        case 't' -> literal("true", true);
        case 'f' -> literal("false", false);
        case 'n' -> literal("null", null);
        default -> number();
      };
    }

    private Map<String, Object> object(int depth) {
      position++;
      var values = new LinkedHashMap<String, Object>();
      if (take('}')) return values;
      do {
        whitespace();
        String name = string();
        expect(':');
        if (values.containsKey(name)) throw new IllegalArgumentException("Duplicate argument");
        values.put(name, value(depth));
      } while (take(','));
      expect('}');
      return values;
    }

    private List<Object> array(int depth) {
      position++;
      var values = new ArrayList<Object>();
      if (take(']')) return values;
      do {
        values.add(value(depth));
      } while (take(','));
      expect(']');
      return values;
    }

    private String string() {
      expect('"');
      StringBuilder value = new StringBuilder();
      while (position < text.length()) {
        char current = text.charAt(position++);
        if (current == '"') return value.toString();
        if (current < 32) throw new IllegalArgumentException("Unescaped control character");
        if (current != '\\') {
          value.append(current);
          continue;
        }
        if (position == text.length()) throw new IllegalArgumentException("Incomplete JSON escape");
        char escape = text.charAt(position++);
        switch (escape) {
          case '"', '\\', '/' -> value.append(escape);
          case 'b' -> value.append('\b');
          case 'f' -> value.append('\f');
          case 'n' -> value.append('\n');
          case 'r' -> value.append('\r');
          case 't' -> value.append('\t');
          case 'u' -> {
            if (position + 4 > text.length()) throw new IllegalArgumentException("Incomplete Unicode escape");
            value.append((char) Integer.parseInt(text.substring(position, position + 4), 16));
            position += 4;
          }
          default -> throw new IllegalArgumentException("Invalid JSON escape");
        }
      }
      throw new IllegalArgumentException("Unterminated JSON string");
    }

    private Object number() {
      int start = position;
      while (position < text.length() && "0123456789-+.eE".indexOf(text.charAt(position)) >= 0) position++;
      if (start == position) throw new IllegalArgumentException("Invalid JSON number");
      String value = text.substring(start, position);
      if (value.indexOf('.') >= 0 || value.indexOf('e') >= 0 || value.indexOf('E') >= 0) return Double.valueOf(value);
      return Long.valueOf(value);
    }

    private Object literal(String expected, Object value) {
      if (!text.startsWith(expected, position)) throw new IllegalArgumentException("Invalid JSON literal");
      position += expected.length();
      return value;
    }

    private boolean take(char expected) {
      whitespace();
      if (position < text.length() && text.charAt(position) == expected) {
        position++;
        return true;
      }
      return false;
    }

    private void expect(char expected) {
      if (!take(expected)) throw new IllegalArgumentException("Expected " + expected);
    }

    private void whitespace() {
      while (position < text.length() && Character.isWhitespace(text.charAt(position))) position++;
    }
  }
}
