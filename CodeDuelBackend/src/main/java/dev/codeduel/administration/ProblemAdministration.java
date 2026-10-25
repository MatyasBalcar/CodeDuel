package dev.codeduel.administration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.codeduel.common.ApiException;
import dev.codeduel.common.AuditLog;
import dev.codeduel.common.Sql;
import dev.codeduel.problems.ProblemCatalog;
import dev.codeduel.submissions.SubmissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Published or queued-for-review content is frozen; an edit thereafter requires a new version. */
@Service
public class ProblemAdministration {
  private final JdbcClient jdbc;
  private final AuditLog audit;
  private final SubmissionService submissions;
  private final ProblemCatalog seeds;
  private final ObjectMapper json;
  private final dev.codeduel.judge.JudgeSettings judge;

  public ProblemAdministration(
      JdbcClient jdbc,
      AuditLog audit,
      SubmissionService submissions,
      ProblemCatalog seeds,
      ObjectMapper json,
      dev.codeduel.judge.JudgeSettings judge) {
    this.jdbc = jdbc;
    this.audit = audit;
    this.submissions = submissions;
    this.seeds = seeds;
    this.json = json;
    this.judge = judge;
  }

  public List<VersionSummary> list() {
    return jdbc.sql(
            """
        SELECT p.id AS problem_id,p.slug,p.title,p.active,pv.id,pv.version,pv.status,
          (SELECT verdict FROM submissions s WHERE s.problem_version_id = pv.id AND purpose = 'VERIFICATION' ORDER BY received_at DESC LIMIT 1) AS verification
        FROM problems p JOIN problem_versions pv ON pv.problem_id = p.id ORDER BY p.title,pv.version DESC
        """)
        .query(
            (rs, row) ->
                new VersionSummary(
                    Sql.uuid(rs, "problem_id"),
                    Sql.uuid(rs, "id"),
                    rs.getString("slug"),
                    rs.getString("title"),
                    rs.getBoolean("active"),
                    rs.getInt("version"),
                    rs.getString("status"),
                    rs.getString("verification")))
        .list();
  }

  @Transactional(readOnly = true)
  public VersionDetail detail(UUID version) {
    var row =
        jdbc.sql(
                "SELECT p.id AS problem_id,p.slug,p.title,p.difficulty,p.tags,p.active,pv.* FROM problems p JOIN problem_versions pv ON pv.problem_id = p.id WHERE pv.id = :id")
            .param("id", version)
            .query(
                (rs, index) ->
                    new VersionDetail(
                        Sql.uuid(rs, "problem_id"),
                        version,
                        rs.getInt("version"),
                        rs.getString("status"),
                        rs.getBoolean("active"),
                        new Draft(
                            rs.getString("slug"),
                            rs.getString("title"),
                            rs.getString("difficulty"),
                            Arrays.asList((String[]) rs.getArray("tags").getArray()),
                            rs.getString("statement"),
                            rs.getString("constraints_text"),
                            rs.getString("input_format"),
                            rs.getString("output_format"),
                            rs.getString("starter_code"),
                            reference(rs.getString("reference_source"), rs.getString("slug")),
                            rs.getInt("time_limit_ms"),
                            rs.getInt("memory_limit_mb"),
                            rs.getInt("duration_seconds"),
                            tests(version)),
                        verification(version)))
            .optional()
            .orElseThrow(ApiException::notFound);
    return row;
  }

  @Transactional
  public UUID create(UUID actor, Draft draft) {
    validate(draft);
    UUID problem = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO problems(id,slug,title,difficulty,tags) VALUES (:id,:slug,:title,:difficulty,string_to_array(:tags,','))")
        .param("id", problem)
        .param("slug", draft.slug())
        .param("title", draft.title())
        .param("difficulty", draft.difficulty())
        .param("tags", String.join(",", draft.tags()))
        .update();
    UUID version = insertVersion(problem, 1, draft);
    audit.append(actor, "PROBLEM_CREATED", "PROBLEM", problem);
    return version;
  }

  @Transactional
  public void save(UUID version, UUID actor, Draft draft) {
    validate(draft);
    UUID problem = lockVersion(version);
    var old = detail(version);
    if (!old.status().equals("DRAFT"))
      throw ApiException.conflict(
          "VERSION_FROZEN", "Clone a new draft to change reviewed or published content.");
    if (!old.content().slug().equals(draft.slug()))
      throw ApiException.conflict(
          "SLUG_IMMUTABLE", "A problem's URL is fixed. Create a new problem for another slug.");
    metadata(problem, draft);
    jdbc.sql(
            """
        UPDATE problem_versions SET statement=:statement,constraints_text=:constraints,input_format=:input,
          output_format=:output,starter_code=:starter,reference_source=:reference,time_limit_ms=:time,
          memory_limit_mb=:memory,duration_seconds=:duration,content_sha256=:checksum WHERE id=:id
        """)
        .param("id", version)
        .param("statement", draft.statement())
        .param("constraints", draft.constraints())
        .param("input", draft.inputFormat())
        .param("output", draft.outputFormat())
        .param("starter", draft.starterCode())
        .param("reference", draft.referenceSource())
        .param("time", draft.timeLimitMs())
        .param("memory", draft.memoryLimitMb())
        .param("duration", draft.durationSeconds())
        .param("checksum", checksum(draft))
        .update();
    jdbc.sql("DELETE FROM test_cases WHERE problem_version_id = :id").param("id", version).update();
    insertTests(version, draft.tests());
    audit.append(actor, "DRAFT_UPDATED", "PROBLEM_VERSION", version);
  }

  @Transactional
  public UUID cloneDraft(UUID version, UUID actor) {
    UUID problem = lockVersion(version);
    Draft draft = detail(version).content();
    int next =
        jdbc.sql("SELECT max(version)+1 FROM problem_versions WHERE problem_id = :id")
            .param("id", problem)
            .query(Integer.class)
            .single();
    UUID created = insertVersion(problem, next, draft);
    audit.append(actor, "VERSION_CLONED", "PROBLEM_VERSION", created);
    return created;
  }

  @Transactional
  public SubmissionService.Receipt review(UUID version, UUID actor) {
    if (judge.mode() != dev.codeduel.judge.JudgeSettings.Mode.GO_JUDGE
        && judge.mode() != dev.codeduel.judge.JudgeSettings.Mode.JUDGE0)
      throw ApiException.conflict(
          "REAL_JUDGE_REQUIRED", "Problem review requires an isolated real Java judge.");
    lockVersion(version);
    var current = detail(version);
    if (!current.status().equals("DRAFT"))
      throw ApiException.conflict(
          "VERSION_FROZEN", "This version has already entered review. Clone it for changes.");
    validate(current.content());
    jdbc.sql("UPDATE problem_versions SET status = 'REVIEW' WHERE id = :id")
        .param("id", version)
        .update();
    var receipt = submissions.verifyReference(version, actor, current.content().referenceSource());
    audit.append(actor, "VERSION_REVIEW_QUEUED", "PROBLEM_VERSION", version);
    return receipt;
  }

  @Transactional
  public void publish(UUID version, UUID actor) {
    UUID problem = lockVersion(version);
    var current = detail(version);
    boolean realVerification =
        jdbc.sql(
                "SELECT EXISTS(SELECT 1 FROM submissions s JOIN judge_jobs j ON j.submission_id=s.id WHERE s.problem_version_id=:id AND s.purpose='VERIFICATION' AND s.verdict='ACCEPTED' AND j.executor_kind IN ('GO_JUDGE','JUDGE0'))")
            .param("id", version)
            .query(Boolean.class)
            .single();
    if (!current.status().equals("REVIEW")
        || !"ACCEPTED".equals(current.verification())
        || !realVerification) {
      throw ApiException.conflict(
          "VERIFICATION_REQUIRED",
          "The frozen reference program must pass every case in the real judge before publication.");
    }
    jdbc.sql(
            "UPDATE problem_versions SET status='PUBLISHED',published_at=clock_timestamp() WHERE id=:id")
        .param("id", version)
        .update();
    jdbc.sql("UPDATE problems SET active=true WHERE id=:id").param("id", problem).update();
    audit.append(actor, "VERSION_PUBLISHED", "PROBLEM_VERSION", version);
  }

  @Transactional
  public void active(UUID problem, UUID actor, boolean active) {
    jdbc.sql("SELECT id FROM problems WHERE id=:id FOR NO KEY UPDATE")
        .param("id", problem)
        .query(UUID.class)
        .optional()
        .orElseThrow(ApiException::notFound);
    jdbc.sql("UPDATE problems SET active=:active WHERE id=:id")
        .param("active", active)
        .param("id", problem)
        .update();
    audit.append(actor, active ? "PROBLEM_ACTIVATED" : "PROBLEM_RETIRED", "PROBLEM", problem);
  }

  private UUID lockVersion(UUID version) {
    UUID problem =
        jdbc.sql("SELECT problem_id FROM problem_versions WHERE id=:id")
            .param("id", version)
            .query(UUID.class)
            .optional()
            .orElseThrow(ApiException::notFound);
    jdbc.sql("SELECT id FROM problems WHERE id=:id FOR NO KEY UPDATE")
        .param("id", problem)
        .query(UUID.class)
        .single();
    jdbc.sql("SELECT id FROM problem_versions WHERE id=:id FOR UPDATE")
        .param("id", version)
        .query(UUID.class)
        .single();
    return problem;
  }

  private UUID insertVersion(UUID problem, int number, Draft draft) {
    UUID version = UUID.randomUUID();
    jdbc.sql(
            """
        INSERT INTO problem_versions(id,problem_id,version,status,statement,constraints_text,input_format,output_format,
          starter_code,reference_source,content_sha256,time_limit_ms,memory_limit_mb,duration_seconds)
        VALUES (:id,:problem,:version,'DRAFT',:statement,:constraints,:input,:output,:starter,:reference,:checksum,:time,:memory,:duration)
        """)
        .param("id", version)
        .param("problem", problem)
        .param("version", number)
        .param("statement", draft.statement())
        .param("constraints", draft.constraints())
        .param("input", draft.inputFormat())
        .param("output", draft.outputFormat())
        .param("starter", draft.starterCode())
        .param("reference", draft.referenceSource())
        .param("checksum", checksum(draft))
        .param("time", draft.timeLimitMs())
        .param("memory", draft.memoryLimitMb())
        .param("duration", draft.durationSeconds())
        .update();
    insertTests(version, draft.tests());
    return version;
  }

  private void metadata(UUID id, Draft draft) {
    jdbc.sql(
            "UPDATE problems SET title=:title,difficulty=:difficulty,tags=string_to_array(:tags,',') WHERE id=:id")
        .param("id", id)
        .param("title", draft.title())
        .param("difficulty", draft.difficulty())
        .param("tags", String.join(",", draft.tags()))
        .update();
  }

  private void insertTests(UUID version, List<Test> tests) {
    for (int i = 0; i < tests.size(); i++) {
      Test test = tests.get(i);
      jdbc.sql(
              "INSERT INTO test_cases(id,problem_version_id,input_data,expected_output,is_public,ordinal) VALUES (:id,:version,:input,:output,:public,:ordinal)")
          .param("id", UUID.randomUUID())
          .param("version", version)
          .param("input", test.input())
          .param("output", test.output())
          .param("public", test.isPublic())
          .param("ordinal", i)
          .update();
    }
  }

  private List<Test> tests(UUID version) {
    return jdbc.sql(
            "SELECT input_data,expected_output,is_public FROM test_cases WHERE problem_version_id=:id ORDER BY ordinal")
        .param("id", version)
        .query(
            (rs, row) ->
                new Test(
                    rs.getString("input_data"),
                    rs.getString("expected_output"),
                    rs.getBoolean("is_public")))
        .list();
  }

  private String verification(UUID version) {
    return jdbc.sql(
            "SELECT verdict FROM submissions WHERE problem_version_id=:id AND purpose='VERIFICATION' ORDER BY received_at DESC LIMIT 1")
        .param("id", version)
        .query(String.class)
        .optional()
        .orElse(null);
  }

  private String reference(String stored, String slug) {
    if (stored != null) return stored;
    return seeds.find(slug).map(ProblemCatalog.Content::referenceSource).orElse("");
  }

  private void validate(Draft draft) {
    if (draft.tests().stream().noneMatch(Test::isPublic)
        || draft.tests().stream().allMatch(Test::isPublic))
      throw ApiException.conflict(
          "TEST_COVERAGE_REQUIRED", "Provide at least one public and one private test.");
    if (draft.referenceSource().getBytes(StandardCharsets.UTF_8).length > 65536
        || draft.starterCode().getBytes(StandardCharsets.UTF_8).length > 65536)
      throw ApiException.conflict("SOURCE_TOO_LARGE", "Java programs must fit in 64 KiB.");
  }

  private String checksum(Draft draft) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(draft)));
    } catch (NoSuchAlgorithmException | java.io.IOException ex) {
      throw new IllegalStateException("Cannot checksum problem content", ex);
    }
  }

  public record VersionSummary(
      UUID problemId,
      UUID versionId,
      String slug,
      String title,
      boolean active,
      int version,
      String status,
      String verification) {}

  public record VersionDetail(
      UUID problemId,
      UUID versionId,
      int version,
      String status,
      boolean active,
      Draft content,
      String verification) {}

  public record Draft(
      @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 80) String slug,
      @NotBlank @Size(max = 120) String title,
      @Pattern(regexp = "EASY|MEDIUM|HARD") @NotNull String difficulty,
      @NotEmpty @Size(max = 8) List<@Pattern(regexp = "[A-Za-z][A-Za-z0-9 ]{0,31}") String> tags,
      @NotBlank @Size(max = 20000) String statement,
      @NotBlank @Size(max = 4000) String constraints,
      @NotBlank @Size(max = 4000) String inputFormat,
      @NotBlank @Size(max = 4000) String outputFormat,
      @NotBlank @Size(max = 65536) String starterCode,
      @NotBlank @Size(max = 65536) String referenceSource,
      @Min(100) @Max(10000) int timeLimitMs,
      @Min(128) @Max(512) int memoryLimitMb,
      @Min(60) @Max(1800) int durationSeconds,
      @NotNull @Size(min = 2, max = 100) List<@Valid Test> tests) {}

  public record Test(
      @NotNull @Size(max = 2097152) String input,
      @NotNull @Size(max = 65536) String output,
      @com.fasterxml.jackson.annotation.JsonProperty("public") boolean isPublic) {}
}
