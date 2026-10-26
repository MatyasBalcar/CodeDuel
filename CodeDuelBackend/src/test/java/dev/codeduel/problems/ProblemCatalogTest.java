package dev.codeduel.problems;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ProblemCatalogTest {
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void oldVersionsKeepExactlyTheSameSerializedContentForTheirChecksums() throws Exception {
    var catalog = new ProblemCatalog(json);
    try (var input = new ClassPathResource("problems/catalog.json").getInputStream()) {
      for (var original : json.readTree(input)) {
        var content =
            catalog
                .find(original.get("slug").asText(), original.get("version").asInt())
                .orElseThrow();
        assertThat(json.writeValueAsBytes(content)).isEqualTo(json.writeValueAsBytes(original));
      }
    }
  }

  @Test
  void everyLatestProblemHasValidFunctionCases() throws Exception {
    var catalog = new ProblemCatalog(json);
    var slugs = catalog.all().stream().map(ProblemCatalog.Content::slug).distinct().toList();
    assertThat(slugs).isNotEmpty();
    for (String slug : slugs) {
      var content = catalog.find(slug).orElseThrow();
      assertThat(content.function()).isNotNull();
      assertThat(content.cases().stream().anyMatch(ProblemCatalog.Case::isPublic)).isTrue();
      assertThat(content.cases().stream().anyMatch(test -> !test.isPublic())).isTrue();
      for (var test : content.cases()) {
        content.function().validateArguments(test.arguments());
        content.function().validateExpected(test.expected());
        assertThat(test.input()).isNull();
        assertThat(test.output()).isNull();
      }
    }
  }
}
