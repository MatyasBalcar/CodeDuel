package dev.codeduel.problems;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class JavaFunctionTest {
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void requiresNamedArgumentsWithTheDeclaredTypes() throws Exception {
    var function =
        new JavaFunction(
            "long",
            List.of(
                new JavaFunction.Parameter("nums", "int[]"),
                new JavaFunction.Parameter("target", "long")));
    function.validateArguments(json.readTree("{\"target\":9223372036854775807,\"nums\":[1,-2]}"));
    function.validateExpected(json.readTree("9223372036854775807"));
    for (String invalid :
        List.of(
            "[1,2]",
            "{\"nums\":[1]}",
            "{\"nums\":[1],\"target\":2,\"extra\":0}",
            "{\"nums\":[1.5],\"target\":2}",
            "{\"nums\":[2147483648],\"target\":2}",
            "{\"nums\":[true],\"target\":2}",
            "{\"nums\":[1],\"target\":9223372036854775808}")) {
      var arguments = json.readTree(invalid);
      assertThatIllegalArgumentException().isThrownBy(() -> function.validateArguments(arguments));
    }
  }

  @Test
  void supportsEmptyAndRaggedArraysWithoutCoercingValues() throws Exception {
    var function = new JavaFunction("int[][]", List.of());
    function.validateExpected(json.readTree("[[],[1,-2]]"));
    for (String invalid : List.of("null", "[null]", "[[\"1\"]]", "[[true]]")) {
      var expected = json.readTree(invalid);
      assertThatIllegalArgumentException().isThrownBy(() -> function.validateExpected(expected));
    }
  }

  @Test
  void rejectsNamesAndTypesThatCannotFormASafeJavaSignature() {
    for (String name : List.of("class", "int", "a);exit(", "a b", "_")) {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () -> new JavaFunction("int", List.of(new JavaFunction.Parameter(name, "int"))));
    }
    assertThatIllegalArgumentException().isThrownBy(() -> new JavaFunction("void", List.of()));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new JavaFunction(
                    "int",
                    List.of(
                        new JavaFunction.Parameter("n", "int"),
                        new JavaFunction.Parameter("n", "long"))));
  }
}
