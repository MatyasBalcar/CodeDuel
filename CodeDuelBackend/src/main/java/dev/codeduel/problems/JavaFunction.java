package dev.codeduel.problems;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.SourceVersion;

public record JavaFunction(String returnType, List<Parameter> parameters) {
  public static final Set<String> TYPES =
      Set.of(
          "int",
          "long",
          "double",
          "boolean",
          "String",
          "int[]",
          "long[]",
          "double[]",
          "boolean[]",
          "String[]",
          "int[][]",
          "long[][]");

  public JavaFunction {
    if (returnType == null
        || !TYPES.contains(returnType)
        || parameters == null
        || parameters.size() > 8)
      throw new IllegalArgumentException(
          "Use a supported return type and at most eight parameters.");
    parameters = List.copyOf(parameters);
    var names = new HashSet<String>();
    for (var parameter : parameters) {
      if (parameter == null
          || parameter.name() == null
          || !parameter.name().matches("[a-zA-Z][a-zA-Z0-9_]{0,39}")
          || !SourceVersion.isIdentifier(parameter.name())
          || SourceVersion.isKeyword(parameter.name())
          || parameter.type() == null
          || !TYPES.contains(parameter.type())
          || !names.add(parameter.name()))
        throw new IllegalArgumentException("Use unique Java parameter names and supported types.");
    }
  }

  public void validateArguments(JsonNode arguments) {
    if (arguments == null || !arguments.isObject() || arguments.size() != parameters.size())
      throw new IllegalArgumentException(
          "Provide one named argument for every function parameter.");
    for (var parameter : parameters)
      validateValue(arguments.get(parameter.name()), parameter.type());
  }

  public void validateExpected(JsonNode expected) {
    validateValue(expected, returnType);
  }

  private static void validateValue(JsonNode value, String type) {
    if (value == null || value.isNull())
      throw new IllegalArgumentException("Test values cannot be null.");
    if (type.endsWith("[]")) {
      if (!value.isArray()) throw new IllegalArgumentException("Expected an array for " + type);
      for (var element : value) validateValue(element, type.substring(0, type.length() - 2));
      return;
    }
    boolean valid =
        switch (type) {
          case "int" -> value.isIntegralNumber() && value.canConvertToInt();
          case "long" -> value.isIntegralNumber() && value.canConvertToLong();
          case "double" -> value.isNumber() && Double.isFinite(value.asDouble());
          case "boolean" -> value.isBoolean();
          case "String" -> value.isTextual();
          default -> false;
        };
    if (!valid) throw new IllegalArgumentException("Test value does not match " + type);
  }

  public static JavaFunction read(String value, ObjectMapper json) {
    if (value == null) return null;
    try {
      return json.readValue(value, JavaFunction.class);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Invalid stored Java function", ex);
    }
  }

  public record Parameter(String name, String type) {}
}
