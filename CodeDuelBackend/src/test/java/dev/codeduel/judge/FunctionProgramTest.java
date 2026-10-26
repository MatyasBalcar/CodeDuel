package dev.codeduel.judge;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.codeduel.problems.JavaFunction;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FunctionProgramTest {
  @TempDir Path directory;
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void callsTheMethodWithArraysAndIgnoresDebugPrinting() throws Exception {
    var function = contract("int[][]", "matrix", "int[][]");
    String actual =
        execute(
            function,
            "public int[][] solve(int[][] matrix) { System.out.println(\"debug\"); return matrix; }",
            "{\"matrix\":[[],[1,-2],[3]]}");
    assertThat(json.readTree(actual)).isEqualTo(json.readTree("[[],[1,-2],[3]]"));
  }

  @Test
  void preservesLongValuesWithoutFloatingPointRounding() throws Exception {
    String actual =
        execute(
            contract("long[]", "values", "long[]"),
            "public long[] solve(long[] values) { return values; }",
            "{\"values\":[9223372036854775807,-9223372036854775808,9007199254740993]}");
    assertThat(actual).isEqualTo("[9223372036854775807,-9223372036854775808,9007199254740993]");
  }

  @Test
  void preservesStringEscapesUnicodeAndEmptyStrings() throws Exception {
    String arguments =
        "{\"words\":[\"\",\"a  b\",\"quote\\\"\\\\\\n\\t\",\"Příliš žluťoučký 🦆\"]}";
    String actual =
        execute(
            contract("String[]", "words", "String[]"),
            "public String[] solve(String[] words) { return words; }",
            arguments);
    assertThat(json.readTree(actual)).isEqualTo(json.readTree(arguments).get("words"));
  }

  @Test
  void passesBooleanAndDoubleArraysAsTheirDeclaredJavaTypes() throws Exception {
    String booleans =
        execute(
            contract("boolean[]", "values", "boolean[]"),
            "public boolean[] solve(boolean[] values) { return values; }",
            "{\"values\":[true,false]}");
    assertThat(booleans).isEqualTo("[true,false]");
    String doubles =
        execute(
            contract("double[]", "values", "double[]"),
            "public double[] solve(double[] values) { return values; }",
            "{\"values\":[1.5,-2e2,0]}");
    assertThat(json.readTree(doubles)).isEqualTo(json.readTree("[1.5,-200.0,0.0]"));
  }

  @Test
  void packagesUserSourceSeparatelyWithoutChangingImportsOrTheClass() throws Exception {
    String source = "import java.util.*;\npublic class Solution {}\n";
    try (var zip =
        new ZipInputStream(
            new ByteArrayInputStream(
                Base64.getDecoder().decode(FunctionProgram.additionalFiles(source))))) {
      assertThat(zip.getNextEntry().getName()).isEqualTo("Solution.java");
      assertThat(new String(zip.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(source);
      assertThat(zip.getNextEntry()).isNull();
    }
  }

  private JavaFunction contract(String result, String name, String type) {
    return new JavaFunction(result, List.of(new JavaFunction.Parameter(name, type)));
  }

  private String execute(JavaFunction function, String method, String arguments) throws Exception {
    Files.writeString(directory.resolve("Main.java"), FunctionProgram.runner(function));
    Files.writeString(
        directory.resolve("Solution.java"), "public class Solution { " + method + " }");
    var diagnostic = new ByteArrayOutputStream();
    int compiled =
        ToolProvider.getSystemJavaCompiler()
            .run(
                null,
                diagnostic,
                diagnostic,
                "--release",
                "21",
                "-encoding",
                "UTF-8",
                "-d",
                directory.toString(),
                "-sourcepath",
                directory.toString(),
                directory.resolve("Main.java").toString());
    assertThat(compiled).as(diagnostic.toString(StandardCharsets.UTF_8)).isZero();
    var process =
        new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp",
                directory.toString(),
                "Main")
            .start();
    try (var input = process.getOutputStream()) {
      input.write(arguments.getBytes(StandardCharsets.UTF_8));
    }
    try {
      assertThat(process.waitFor(10, TimeUnit.SECONDS)).isTrue();
      assertThat(process.exitValue())
          .as(new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8))
          .isZero();
      return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    } finally {
      process.destroyForcibly();
    }
  }
}
