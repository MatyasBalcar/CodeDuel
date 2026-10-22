package dev.codeduel.problems;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
@ConditionalOnProperty(name = "codeduel.seed-enabled", havingValue = "true", matchIfMissing = true)
public class ProblemSeed implements ApplicationRunner {
  private final JdbcClient jdbc;
  private final ProblemCatalog catalog;
  private final ObjectMapper json;

  public ProblemSeed(JdbcClient jdbc, ProblemCatalog catalog, ObjectMapper json) {
    this.jdbc = jdbc;
    this.catalog = catalog;
    this.json = json;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    jdbc.sql("SELECT pg_advisory_xact_lock(772406002)").query().singleRow();
    for (var content : catalog.all()) {
      String checksum =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(content)));
      var existing =
          jdbc.sql(
                  "SELECT pv.content_sha256 FROM problem_versions pv JOIN problems p ON p.id = pv.problem_id WHERE p.slug = :slug AND pv.version = :version")
              .param("slug", content.slug())
              .param("version", content.version())
              .query(String.class)
              .optional();
      if (existing.isPresent()) {
        if (!existing.get().equals(checksum))
          throw new IllegalStateException(
              "Seed content changed without a new version: " + content.slug());
        continue;
      }
      UUID problemId = id(content.slug());
      UUID versionId = id(content.slug() + ":" + content.version());
      jdbc.sql(
              "INSERT INTO problems(id, slug, title, difficulty, tags) VALUES (:id, :slug, :title, :difficulty, string_to_array(:tags, ',')) ON CONFLICT (id) DO NOTHING")
          .param("id", problemId)
          .param("slug", content.slug())
          .param("title", content.title())
          .param("difficulty", content.difficulty())
          .param("tags", String.join(",", content.tags()))
          .update();
      jdbc.sql(
              """
          INSERT INTO problem_versions(id, problem_id, version, status, statement, constraints_text,
              input_format, output_format, starter_code, content_sha256, time_limit_ms, memory_limit_mb, duration_seconds, reference_source)
          VALUES (:id, :problem, :version, 'DRAFT', :statement, :constraints, :input, :output, :starter, :checksum, :time, :memory, :duration, :reference)
          """)
          .param("id", versionId)
          .param("problem", problemId)
          .param("version", content.version())
          .param("statement", content.statement())
          .param("constraints", content.constraints())
          .param("input", content.inputFormat())
          .param("output", content.outputFormat())
          .param("starter", content.starterCode())
          .param("checksum", checksum)
          .param("time", content.timeLimitMs())
          .param("memory", content.memoryLimitMb())
          .param("reference", content.referenceSource())
          .param("duration", content.durationSeconds())
          .update();
      for (int ordinal = 0; ordinal < content.cases().size(); ordinal++) {
        var test = content.cases().get(ordinal);
        jdbc.sql(
                "INSERT INTO test_cases(id, problem_version_id, input_data, expected_output, is_public, ordinal) VALUES (:id, :version, :input, :output, :public, :ordinal)")
            .param("id", id(versionId + ":" + ordinal))
            .param("version", versionId)
            .param("input", test.input())
            .param("output", test.output())
            .param("public", test.isPublic())
            .param("ordinal", ordinal)
            .update();
      }
      jdbc.sql(
              "UPDATE problem_versions SET status = 'PUBLISHED', published_at = clock_timestamp() WHERE id = :id")
          .param("id", versionId)
          .update();
    }
  }

  public static UUID id(String value) {
    return UUID.nameUUIDFromBytes(("codeduel:" + value).getBytes(StandardCharsets.UTF_8));
  }
}
