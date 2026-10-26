package dev.codeduel.judge;

import dev.codeduel.problems.JavaFunction;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.core.io.ClassPathResource;

public final class FunctionProgram {
  private static final String TEMPLATE = template();

  private FunctionProgram() {}

  public static String runner(JavaFunction function) {
    String arguments = function.parameters().stream()
        .map(parameter -> "(" + parameter.type() + ") convert(arguments.get(\""
            + parameter.name() + "\"), \"" + parameter.type() + "\")")
        .collect(java.util.stream.Collectors.joining(", "));
    return TEMPLATE.replace("__RETURN_TYPE__", function.returnType()).replace("__ARGUMENTS__", arguments);
  }

  public static String additionalFiles(String source) {
    try {
      var bytes = new ByteArrayOutputStream();
      try (var zip = new ZipOutputStream(bytes)) {
        var entry = new ZipEntry("Solution.java");
        entry.setTime(0);
        zip.putNextEntry(entry);
        zip.write(source.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
      }
      return Base64.getEncoder().encodeToString(bytes.toByteArray());
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot prepare the Java solution", ex);
    }
  }

  private static String template() {
    try (var input = new ClassPathResource("judge/FunctionRunner.java").getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot load the function runner", ex);
    }
  }
}
