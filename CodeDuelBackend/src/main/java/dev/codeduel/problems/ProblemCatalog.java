package dev.codeduel.problems;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Trusted seed content; reference programs and private cases never appear in public DTOs. */
@Component
public class ProblemCatalog {
  public static final String WRONG_SOURCE =
      "public class Main { public static void main(String[] args) { System.out.println(\"demo-wrong\"); } }\n";
  private final List<Content> content;

  public ProblemCatalog(ObjectMapper json) throws IOException {
    try (var input = new ClassPathResource("problems/catalog.json").getInputStream()) {
      content = List.copyOf(json.readValue(input, new TypeReference<List<Content>>() {}));
    }
  }

  public List<Content> all() {
    return content;
  }

  public record Content(
      String slug,
      String title,
      String difficulty,
      List<String> tags,
      int version,
      String statement,
      String constraints,
      String inputFormat,
      String outputFormat,
      String starterCode,
      String referenceSource,
      int timeLimitMs,
      int memoryLimitMb,
      int durationSeconds,
      List<Case> cases) {}

  public record Case(String input, String output, boolean isPublic) {
    public Case(
        @com.fasterxml.jackson.annotation.JsonProperty("input") String input,
        @com.fasterxml.jackson.annotation.JsonProperty("output") String output,
        @com.fasterxml.jackson.annotation.JsonProperty("public") boolean isPublic) {
      this.input = input;
      this.output = output;
      this.isPublic = isPublic;
    }
  }
}
