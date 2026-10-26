package dev.codeduel.problems;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
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
      var versions = new java.util.ArrayList<>(json.readValue(input, new TypeReference<List<Content>>() {}));
      try (var functions = new ClassPathResource("problems/functions.json").getInputStream()) {
        versions.addAll(json.readValue(functions, new TypeReference<List<Content>>() {}));
      }
      content = List.copyOf(versions);
    }
  }

  public List<Content> all() {
    return content;
  }

  public Optional<Content> find(String slug) {
    for (int index = content.size() - 1; index >= 0; index--) {
      var problem = content.get(index);
      if (problem.slug().equals(slug)) return Optional.of(problem);
    }
    return Optional.empty();
  }

  public Optional<Content> find(String slug, int version) {
    for (var problem : content) {
      if (problem.slug().equals(slug) && problem.version() == version) return Optional.of(problem);
    }
    return Optional.empty();
  }

  public static String wrongSource(Content problem) {
    return problem.function() == null ? WRONG_SOURCE : problem.starterCode();
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
      List<Case> cases,
      @JsonInclude(JsonInclude.Include.NON_NULL) JavaFunction function) {}

  public record Case(
      @JsonInclude(JsonInclude.Include.NON_NULL) String input,
      @JsonInclude(JsonInclude.Include.NON_NULL) String output,
      @com.fasterxml.jackson.annotation.JsonProperty("public") boolean isPublic,
      @JsonInclude(JsonInclude.Include.NON_NULL) JsonNode arguments,
      @JsonInclude(JsonInclude.Include.NON_NULL) JsonNode expected) {
    public String executionInput() {
      return arguments == null ? input : arguments.toString();
    }

    public String expectedOutput() {
      return expected == null ? output : expected.toString();
    }
  }
}
