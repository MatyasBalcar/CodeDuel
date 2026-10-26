package dev.codeduel.judge;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

final class JudgeOutput {
  private static final ObjectMapper JSON = new ObjectMapper()
      .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

  private JudgeOutput() {}

  static boolean matches(String actual, String expected, boolean function) {
    if (!function) return Judge0Gateway.canonical(actual).equals(Judge0Gateway.canonical(expected));
    try {
      return equivalent(JSON.readTree(actual), JSON.readTree(expected));
    } catch (IOException | IllegalArgumentException ex) {
      return false;
    }
  }

  private static boolean equivalent(JsonNode actual, JsonNode expected) {
    if (actual == null || expected == null) return false;
    if (actual.isNumber() && expected.isNumber())
      return actual.decimalValue().compareTo(expected.decimalValue()) == 0;
    if (actual.isArray() && expected.isArray()) {
      if (actual.size() != expected.size()) return false;
      for (int index = 0; index < actual.size(); index++) {
        if (!equivalent(actual.get(index), expected.get(index))) return false;
      }
      return true;
    }
    return actual.equals(expected);
  }
}
