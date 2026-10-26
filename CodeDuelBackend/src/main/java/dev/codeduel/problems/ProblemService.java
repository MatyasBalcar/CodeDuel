package dev.codeduel.problems;

import dev.codeduel.common.ApiException;
import dev.codeduel.common.Sql;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class ProblemService {
  private final JdbcClient jdbc;
  private final ObjectMapper json;

  public ProblemService(JdbcClient jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public List<Summary> catalog() {
    return jdbc.sql(
            """
        SELECT p.id, p.slug, p.title, p.difficulty, p.tags, pv.duration_seconds
        FROM problems p JOIN LATERAL (SELECT duration_seconds FROM problem_versions WHERE problem_id = p.id AND status = 'PUBLISHED' ORDER BY version DESC LIMIT 1) pv ON true
        WHERE p.active ORDER BY p.difficulty, p.title
        """)
        .query(
            (rs, row) ->
                new Summary(
                    Sql.uuid(rs, "id"),
                    rs.getString("slug"),
                    rs.getString("title"),
                    rs.getString("difficulty"),
                    Arrays.asList((String[]) rs.getArray("tags").getArray()),
                    rs.getInt("duration_seconds")))
        .list();
  }

  public Detail published(String slug) {
    UUID id =
        jdbc.sql(
                "SELECT pv.id FROM problem_versions pv JOIN problems p ON p.id = pv.problem_id WHERE p.slug = :slug AND p.active AND pv.status = 'PUBLISHED' ORDER BY pv.version DESC LIMIT 1")
            .param("slug", slug)
            .query(UUID.class)
            .optional()
            .orElseThrow(ApiException::notFound);
    return version(id);
  }

  public Detail version(UUID versionId) {
    return jdbc.sql(
            "SELECT p.slug, p.title, p.difficulty, p.tags, pv.* FROM problem_versions pv JOIN problems p ON p.id = pv.problem_id WHERE pv.id = :id AND pv.status = 'PUBLISHED'")
        .param("id", versionId)
        .query(
            (rs, row) -> {
                var function = JavaFunction.read(rs.getString("function_contract"), json);
                return new Detail(
                    versionId,
                    rs.getString("slug"),
                    rs.getString("title"),
                    rs.getString("difficulty"),
                    Arrays.asList((String[]) rs.getArray("tags").getArray()),
                    rs.getInt("version"),
                    rs.getString("statement"),
                    rs.getString("constraints_text"),
                    rs.getString("input_format"),
                    rs.getString("output_format"),
                    rs.getString("starter_code"),
                    rs.getInt("time_limit_ms"),
                    rs.getInt("memory_limit_mb"),
                    rs.getInt("duration_seconds"),
                    examples(versionId, function != null), function);
            })
        .optional()
        .orElseThrow(ApiException::notFound);
  }

  private List<Example> examples(UUID versionId, boolean function) {
    return jdbc.sql(
            "SELECT COALESCE(arguments::text,input_data) AS input_data, COALESCE(expected_value::text,expected_output) AS expected_output FROM test_cases WHERE problem_version_id = :id AND is_public ORDER BY ordinal")
        .param("id", versionId)
        .query(
            (rs, row) -> new Example(exampleValue(rs.getString("input_data"), function), exampleValue(rs.getString("expected_output"), function)))
        .list();
  }

  private JsonNode exampleValue(String value, boolean function) {
    if (!function) return json.getNodeFactory().textNode(value);
    try {
      return json.readTree(value);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Invalid function example", ex);
    }
  }

  /**
   * Prefer challenges not recently encountered by either participant, including cancelled rooms.
   */
  public UUID select(UUID first, UUID second) {
    return jdbc.sql(
            """
        SELECT pv.id FROM problem_versions pv JOIN problems p ON p.id = pv.problem_id
        WHERE p.active AND pv.status = 'PUBLISHED'
          AND pv.version = (SELECT max(version) FROM problem_versions WHERE problem_id = p.id AND status = 'PUBLISHED')
        ORDER BY (SELECT count(*) FROM battles b JOIN battle_participants bp ON bp.battle_id = b.id
          JOIN problem_versions seen ON seen.id = b.problem_version_id
          WHERE seen.problem_id = p.id AND bp.user_id IN (:first, :second)
            AND b.created_at > clock_timestamp() - interval '30 days') ASC, random()
        LIMIT 1
        """)
        .param("first", first)
        .param("second", second == null ? first : second)
        .query(UUID.class)
        .optional()
        .orElseThrow(
            () -> ApiException.conflict("NO_PROBLEMS", "No published challenges are available."));
  }

  public record Summary(
      UUID id,
      String slug,
      String title,
      String difficulty,
      List<String> tags,
      int durationSeconds) {}

  public record Detail(
      UUID versionId,
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
      int timeLimitMs,
      int memoryLimitMb,
      int durationSeconds,
      List<Example> examples,
      JavaFunction function) {}

  public record Example(JsonNode input, JsonNode output) {}
}
