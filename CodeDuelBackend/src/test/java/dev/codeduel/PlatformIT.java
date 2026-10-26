package dev.codeduel;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.codeduel.auth.AccountPrincipal;
import dev.codeduel.auth.AccountService;
import dev.codeduel.battles.*;
import dev.codeduel.judge.*;
import dev.codeduel.matchmaking.MatchmakingService;
import dev.codeduel.problems.ProblemCatalog;
import dev.codeduel.submissions.*;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "codeduel.scheduling-enabled=false",
      "codeduel.worker-enabled=false",
      "codeduel.battle.start-delay-seconds=0"
    })
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Testcontainers
class PlatformIT {
  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.11-alpine");

  private static final String PASSWORD = "integration-password-2026";
  @Autowired AccountService accounts;
  @Autowired dev.codeduel.auth.AccountRecovery recovery;
  @Autowired dev.codeduel.auth.AccountDataService accountData;
  @Autowired dev.codeduel.auth.MailOutbox mail;
  @Autowired dev.codeduel.administration.ProblemAdministration administration;
  @Autowired BattleService battles;
  @Autowired BattleQueries queries;
  @Autowired MatchmakingService matchmaking;
  @Autowired SubmissionService submissions;
  @Autowired FinalizeBattleService finalizer;
  @Autowired JudgeJobStore jobs;
  @Autowired JudgeGateway gateway;
  @Autowired MeterRegistry metrics;
  @Autowired SessionGuard sessions;
  @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
  @Autowired ProblemCatalog catalog;
  @Autowired dev.codeduel.problems.ProblemSeed problemSeed;
  @Autowired JdbcClient jdbc;
  @Autowired MockMvc http;
  @Autowired ObjectMapper json;
  UUID first;
  UUID second;

  @BeforeEach
  void reset() throws Exception {
    jdbc.sql(
            "TRUNCATE spring_session, audit_log, problems, problem_versions, test_cases, users, battles, active_sessions, matchmaking_entries, battle_participants, submissions, judge_jobs, submission_case_results, rating_events, battle_events, request_limits CASCADE")
        .update();
    problemSeed.run(null);
    first = accounts.register("first", "first@example.invalid", PASSWORD);
    second = accounts.register("second", "second@example.invalid", PASSWORD);
  }

  @Test
  void privateRoomConsumesInviteAndPinsIdenticalProblemWithoutRating() {
    var room = battles.createPrivate(first);
    assertThat(queries.snapshot(room.battleId(), first).problem()).isNull();
    assertThat(battles.joinPrivate(second, room.inviteCode())).isEqualTo(room.battleId());
    assertThat(battles.joinPrivate(second, room.inviteCode())).isEqualTo(room.battleId());
    battles.ready(first, room.battleId());
    battles.ready(second, room.battleId());
    var one = queries.snapshot(room.battleId(), first);
    var two = queries.snapshot(room.battleId(), second);
    assertThat(one.problem().versionId()).isEqualTo(two.problem().versionId());
    assertThat(one.startsAt()).isEqualTo(two.startsAt());
    assertThat(one.endsAt()).isEqualTo(two.endsAt());
    battles.forfeit(first, room.battleId());
    assertThat(queries.snapshot(room.battleId(), first).winnerId()).isEqualTo(second);
    assertThat(count("rating_events")).isZero();
  }

  @Test
  void duplicateQueueEntryAndConcurrentPairingCannotMatchAnyoneTwice() throws Exception {
    together(() -> matchmaking.enter(first), () -> matchmaking.enter(first));
    matchmaking.enter(second);
    together(matchmaking::pair, matchmaking::pair);
    assertThat(count("battles")).isEqualTo(1);
    assertThat(count("battle_participants")).isEqualTo(2);
    assertThat(count("matchmaking_entries")).isZero();
    assertThat(matchmaking.state(first).battleId()).isEqualTo(matchmaking.state(second).battleId());
  }

  @RepeatedTest(5)
  void cancelRacingPairingPreservesActiveSessionInvariant() throws Exception {
    matchmaking.enter(first);
    matchmaking.enter(second);
    together(() -> matchmaking.cancel(first), matchmaking::pair);
    var state = matchmaking.state(first);
    if (state.status().equals("MATCHED")) {
      assertThat(queries.snapshot(state.battleId(), first).players()).hasSize(2);
      assertThat(count("matchmaking_entries")).isZero();
    } else {
      assertThat(state.status()).isEqualTo("IDLE");
      assertThat(count("battles")).isZero();
    }
  }

  @Test
  void userSessionLockAllowsConcurrentParticipantForeignKeyChecks() {
    matchmaking.enter(first);
    matchmaking.enter(second);
    try (var executor = Executors.newSingleThreadExecutor()) {
      new org.springframework.transaction.support.TransactionTemplate(transactions)
          .execute(
              status -> {
                sessions.lockUser(first);
                var matching = executor.submit(matchmaking::pair);
                assertThatCode(() -> assertThat(matching.get(5, TimeUnit.SECONDS)).isEqualTo(1))
                    .doesNotThrowAnyException();
                return null;
              });
    }
    assertThat(count("battles")).isEqualTo(1);
  }

  @Test
  void activePrivateRoomBlocksQueueAndOtherRooms() {
    battles.createPrivate(first);
    assertThatThrownBy(() -> matchmaking.enter(first))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
    assertThatThrownBy(() -> battles.createPrivate(first))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
    assertThat(count("battles")).isEqualTo(1);
  }

  @Test
  void readyAfterServerDeadlineCannotStartBattle() {
    var room = battles.createPrivate(first);
    battles.joinPrivate(second, room.inviteCode());
    jdbc.sql(
            "UPDATE battles SET ready_deadline = clock_timestamp() - interval '1 second' WHERE id = :id")
        .param("id", room.battleId())
        .update();
    assertThatThrownBy(() -> battles.ready(first, room.battleId()))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
    finalizer.attempt(room.battleId());
    assertThat(queries.snapshot(room.battleId(), first).status()).isEqualTo(BattleStatus.CANCELLED);
    assertThat(count("active_sessions")).isZero();
  }

  @Test
  void earlierReceivedAcceptedWinsWhenJudgeCompletionOrderIsReversed() {
    UUID id = ranked();
    var early = submit(id, first);
    var late = submit(id, second);
    verdict(late.submissionId(), "ACCEPTED");
    finalizer.attempt(id);
    assertThat(queries.snapshot(id, first).status()).isEqualTo(BattleStatus.JUDGING);
    assertThat(count("rating_events")).isZero();
    verdict(early.submissionId(), "ACCEPTED");
    finalizer.attempt(id);
    var result = queries.snapshot(id, first);
    assertThat(result.winnerId()).isEqualTo(first);
    assertThat(result.players().get(0).delta()).isEqualByComparingTo("20");
    assertThat(result.players().get(1).delta()).isEqualByComparingTo("-20");
  }

  @Test
  void concurrentFinalizationAppliesRatingsExactlyOnce() throws Exception {
    UUID id = ranked();
    var early = submit(id, first);
    verdict(early.submissionId(), "ACCEPTED");
    together(
        () -> {
          finalizer.attempt(id);
          return null;
        },
        () -> {
          finalizer.attempt(id);
          return null;
        });
    finalizer.attempt(id);
    assertThat(count("rating_events")).isEqualTo(2);
    assertThat(
            jdbc.sql("SELECT ranked_games FROM rating_profiles WHERE user_id = :user")
                .param("user", first)
                .query(Integer.class)
                .single())
        .isEqualTo(1);
    assertThat(
            jdbc.sql(
                    "SELECT count(*) FROM battle_events WHERE battle_id = :id AND event_type = 'BATTLE_FINISHED'")
                .param("id", id)
                .query(Integer.class)
                .single())
        .isEqualTo(1);
  }

  @Test
  void inTimeSubmissionCanFinishAfterDeadlineButNewSubmissionsCannotEnter() {
    UUID id = ranked();
    var accepted = submit(id, first);
    expire(id);
    finalizer.attempt(id);
    assertThat(queries.snapshot(id, first).status()).isEqualTo(BattleStatus.JUDGING);
    assertThatThrownBy(() -> submit(id, second))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
    verdict(accepted.submissionId(), "ACCEPTED");
    finalizer.attempt(id);
    assertThat(queries.snapshot(id, first).winnerId()).isEqualTo(first);
  }

  @Test
  void noAcceptedSubmissionAtDeadlineIsRankedDraw() {
    UUID id = ranked();
    expire(id);
    finalizer.attempt(id);
    var result = queries.snapshot(id, first);
    assertThat(result.winnerId()).isNull();
    assertThat(result.finalizationReason()).isEqualTo("DEADLINE_DRAW");
    assertThat(result.players()).allSatisfy(p -> assertThat(p.delta()).isEqualByComparingTo("0"));
    assertThat(count("rating_events")).isEqualTo(2);
  }

  @Test
  void publicExampleRunNeverWinsOfficialBattle() {
    UUID id = ranked();
    var run = submissions.submit(id, first, "JAVA_21", source(id), false);
    verdict(run.submissionId(), "ACCEPTED");
    finalizer.attempt(id);
    assertThat(queries.snapshot(id, first).status()).isEqualTo(BattleStatus.IN_PROGRESS);
  }

  @Test
  void publishedTestsVersionsAndSourceCannotBeMutated() {
    UUID id = ranked();
    var receipt = submit(id, first);
    UUID version = queries.snapshot(id, first).problem().versionId();
    assertThatThrownBy(
            () ->
                jdbc.sql("UPDATE problem_versions SET statement = 'changed' WHERE id = :id")
                    .param("id", version)
                    .update())
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.sql(
                        "UPDATE test_cases SET expected_output = 'changed' WHERE problem_version_id = :id")
                    .param("id", version)
                    .update())
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.sql("UPDATE submissions SET source_code = 'changed' WHERE id = :id")
                    .param("id", receipt.submissionId())
                    .update())
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.sql(
                        "UPDATE submissions SET problem_version_id = (SELECT id FROM problem_versions WHERE id <> :version LIMIT 1) WHERE id = :id")
                    .param("version", version)
                    .param("id", receipt.submissionId())
                    .update())
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void concurrentJobClaimsAndExpiredLeaseFencingSurviveWorkerCrash() throws Exception {
    UUID id = ranked();
    submit(id, first);
    var results = together(jobs::claim, jobs::claim);
    assertThat(results.stream().filter(Optional -> Optional.isPresent()).count()).isEqualTo(1);
    var stale =
        results.stream()
            .filter(java.util.Optional::isPresent)
            .findFirst()
            .orElseThrow()
            .orElseThrow();
    jdbc.sql(
            "UPDATE judge_jobs SET lease_until = clock_timestamp() - interval '1 second' WHERE id = :id")
        .param("id", stale.id())
        .update();
    var current = jobs.claim().orElseThrow();
    var test = jobs.remaining(current).get(0);
    assertThat(current.leaseToken()).isNotEqualTo(stale.leaseToken());
    assertThat(jobs.saveToken(stale, test.id(), "stale-token")).isFalse();
    assertThat(jobs.saveToken(current, test.id(), "current-token")).isTrue();
  }

  @Test
  void realWorkerPipelinePersistsFixtureVerdictAndFinalizesRankedBattle() {
    UUID id = ranked();
    submit(id, first);
    var worker = new JudgeWorker(jobs, gateway, metrics);
    for (int i = 0; i < 200 && !queries.snapshot(id, first).status().terminal(); i++) {
      jdbc.sql("UPDATE judge_jobs SET next_retry_at = clock_timestamp() WHERE status = 'PENDING'")
          .update();
      worker.tick();
    }
    assertThat(queries.snapshot(id, first).winnerId()).isEqualTo(first);
    assertThat(jdbc.sql("SELECT status FROM judge_jobs").query(String.class).single())
        .isEqualTo("COMPLETED");
    assertThat(count("rating_events")).isEqualTo(2);
  }

  @Test
  void hiddenExecutionOutputIsNeverPersistedAsPublicFeedback() {
    UUID id = ranked();
    var receipt = submit(id, first);
    var job = jobs.claim().orElseThrow();
    var hidden =
        jdbc.sql(
                "SELECT r.test_case_id FROM submission_case_results r JOIN test_cases t ON t.id = r.test_case_id WHERE r.submission_id = :id AND NOT t.is_public LIMIT 1")
            .param("id", receipt.submissionId())
            .query(UUID.class)
            .single();
    assertThat(
            jobs.saveResult(
                job,
                hidden,
                new JudgeGateway.Result(Verdict.WRONG_ANSWER, 1, 1, "", "PRIVATE_INPUT_ECHO")))
        .isTrue();
    assertThat(
            jdbc.sql(
                    "SELECT public_stdout FROM submission_case_results WHERE submission_id = :id AND test_case_id = :test")
                .param("id", receipt.submissionId())
                .param("test", hidden)
                .query(String.class)
                .single())
        .isEmpty();
    assertThat(submissions.own(receipt.submissionId(), first).publicResults())
        .noneMatch(result -> result.stdout().contains("PRIVATE_INPUT_ECHO"));
  }

  @Test
  void compiledProgramCheckpointSurvivesAClaimAndResetsOnExecutorFailure() {
    UUID id = ranked();
    submit(id, first);
    var job = jobs.claim().orElseThrow();
    assertThat(jobs.saveProgram(job, "compiled-artifact")).isTrue();
    jobs.finishOrReschedule(job);
    jdbc.sql("UPDATE judge_jobs SET next_retry_at = clock_timestamp()").update();
    var next = jobs.claim().orElseThrow();
    assertThat(next.programToken()).isEqualTo("compiled-artifact");
    assertThat(jobs.remaining(next).get(0).executorToken()).isEqualTo("compiled-artifact");
    jobs.fail(next);
    jdbc.sql("UPDATE judge_jobs SET next_retry_at = clock_timestamp()").update();
    assertThat(jobs.claim().orElseThrow().programToken()).isNull();
  }

  @Test
  @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(
      named = "CODEDUEL_EXECUTOR_URL",
      matches = ".+")
  void realJavaWorkerChecksHiddenCasesAndFinalizesEloExactlyOnce() {
    UUID id = ranked();
    var receipt = submit(id, first);
    var real =
        new GoJudgeGateway(
            new JudgeSettings(
                JudgeSettings.Mode.GO_JUDGE,
                System.getenv("CODEDUEL_EXECUTOR_URL"),
                System.getenv("CODEDUEL_EXECUTOR_TOKEN"),
                0,
                ""),
            json);
    var worker = new JudgeWorker(jobs, real, metrics);
    for (int i = 0; i < 200 && !queries.snapshot(id, first).status().terminal(); i++) {
      jdbc.sql("UPDATE judge_jobs SET next_retry_at = clock_timestamp() WHERE status = 'PENDING'")
          .update();
      worker.tick();
    }
    assertThat(submissions.own(receipt.submissionId(), first).verdict())
        .isEqualTo(Verdict.ACCEPTED);
    assertThat(queries.snapshot(id, first).winnerId()).isEqualTo(first);
    finalizer.attempt(id);
    finalizer.attempt(id);
    assertThat(count("rating_events")).isEqualTo(2);
    assertThat(
            jdbc.sql(
                    "SELECT count(*) FROM submission_case_results WHERE submission_id = :id AND verdict = 'ACCEPTED'")
                .param("id", receipt.submissionId())
                .query(Integer.class)
                .single())
        .isGreaterThan(2);
  }

  @Test
  void unrecoverableInfrastructureFailureCancelsWithoutRating() {
    UUID id = ranked();
    var receipt = submit(id, first);
    for (int i = 0; i < 3; i++) {
      jdbc.sql("UPDATE judge_jobs SET next_retry_at = clock_timestamp() WHERE status = 'PENDING'")
          .update();
      jobs.fail(jobs.claim().orElseThrow());
    }
    assertThat(submissions.own(receipt.submissionId(), first).verdict())
        .isEqualTo(Verdict.JUDGE_ERROR);
    assertThat(queries.snapshot(id, first).status()).isEqualTo(BattleStatus.CANCELLED);
    assertThat(count("rating_events")).isZero();
  }

  @Test
  void sessionsUseCsrfAndMembershipProtectsBattleStreamAndSource() throws Exception {
    var csrfResponse =
        http.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
    Cookie session = csrfResponse.getResponse().getCookies()[0];
    var token = json.readTree(csrfResponse.getResponse().getContentAsString());
    String credentials = json.writeValueAsString(Map.of("username", "first", "password", PASSWORD));
    http.perform(
            post("/api/v1/auth/login")
                .cookie(session)
                .contentType("application/json")
                .content(credentials))
        .andExpect(status().isForbidden());
    var loggedIn =
        http.perform(
                post("/api/v1/auth/login")
                    .cookie(session)
                    .header(token.get("headerName").asText(), token.get("token").asText())
                    .contentType("application/json")
                    .content(credentials))
            .andExpect(status().isOk())
            .andReturn();
    session = loggedIn.getResponse().getCookies()[0];
    assertThat(
            jdbc.sql("SELECT count(*) FROM spring_session WHERE principal_name = 'first'")
                .query(Integer.class)
                .single())
        .isEqualTo(1);
    http.perform(get("/api/v1/auth/me").cookie(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("username").value("first"));
    UUID id = ranked();
    var receipt = submit(id, first);
    UUID stranger = accounts.register("stranger", "stranger@example.invalid", PASSWORD);
    var principal = new AccountPrincipal(stranger, "stranger", "", "USER");
    http.perform(get("/api/v1/battles/" + id).with(user(principal)))
        .andExpect(status().isNotFound());
    http.perform(get("/api/v1/battles/" + id + "/events").with(user(principal)))
        .andExpect(status().isNotFound());
    http.perform(
            get("/api/v1/submissions/" + receipt.submissionId())
                .with(user(new AccountPrincipal(second, "second", "", "USER"))))
        .andExpect(status().isNotFound());
    String snapshot =
        http.perform(get("/api/v1/battles/" + id).cookie(session))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(snapshot)
        .doesNotContain("sourceCode", "expected_output", "input_data", "referenceSource");
    http.perform(post("/api/v1/auth/logout").cookie(session).with(csrf()))
        .andExpect(status().isNoContent());
    http.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void libraryReturnsFunctionArgumentsBeforeStartingAnyMatch() throws Exception {
    int versions = count("problem_versions");
    problemSeed.run(null);
    assertThat(count("problem_versions")).isEqualTo(versions);
    http.perform(get("/api/v1/problems/two-sum"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("version").value(2))
        .andExpect(jsonPath("function.returnType").value("int[]"))
        .andExpect(jsonPath("function.parameters[0].name").value("nums"))
        .andExpect(jsonPath("examples[0].input.nums").isArray())
        .andExpect(jsonPath("examples[0].input.target").isNumber())
        .andExpect(jsonPath("examples[0].output").isArray())
        .andExpect(jsonPath("referenceSource").doesNotExist())
        .andExpect(jsonPath("cases").doesNotExist());
    http.perform(get("/api/v1/problems/palindrome-check"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("examples[0].output").isBoolean());
    assertThat(count("battles")).isZero();
  }

  @Test
  void functionDraftsRetainTheirContractAndStructuredCases() throws Exception {
    UUID published = dev.codeduel.problems.ProblemSeed.id("two-sum:2");
    UUID draft = administration.cloneDraft(published, first);
    var content = administration.detail(draft).content();
    assertThat(content.function()).isEqualTo(catalog.find("two-sum").orElseThrow().function());
    assertThat(json.readTree(content.tests().getFirst().input()).get("nums").isArray()).isTrue();
    assertThat(json.readTree(content.tests().getFirst().output()).isArray()).isTrue();
    administration.save(draft, first, content);
    assertThat(administration.detail(draft).content()).isEqualTo(content);
    var invalid = json.valueToTree(content);
    ((com.fasterxml.jackson.databind.node.ObjectNode) invalid.get("tests").get(0))
        .put("input", "{\"nums\":[1],\"target\":\"not a number\"}");
    var invalidDraft =
        json.treeToValue(invalid, dev.codeduel.administration.ProblemAdministration.Draft.class);
    assertThatThrownBy(() -> administration.save(draft, first, invalidDraft))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
    assertThatThrownBy(
            () ->
                jdbc.sql("UPDATE problem_versions SET function_contract=NULL WHERE id=:id")
                    .param("id", published)
                    .update())
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void legacyVersionsKeepWorkingAlongsideFunctionVersions() {
    var problem = catalog.find("balanced-relay", 1).orElseThrow();
    var receipt =
        submissions.practice(
            dev.codeduel.problems.ProblemSeed.id("balanced-relay:1"),
            first,
            "JAVA_21",
            problem.referenceSource(),
            false);
    drainFixtures();
    assertThat(submissions.own(receipt.submissionId(), first).verdict())
        .isEqualTo(Verdict.ACCEPTED);
  }

  @Test
  void practiceUsesHiddenCasesButExamplesDoNotAndNeitherChangesRatings() throws Exception {
    var problem = catalog.find("balanced-relay").orElseThrow();
    UUID version =
        jdbc.sql(
                "SELECT pv.id FROM problem_versions pv JOIN problems p ON p.id=pv.problem_id WHERE p.slug=:slug AND pv.version=:version")
            .param("slug", problem.slug())
            .param("version", problem.version())
            .query(UUID.class)
            .single();
    var run = submissions.practice(version, first, "JAVA_21", problem.referenceSource(), true);
    var full = submissions.practice(version, first, "JAVA_21", problem.referenceSource(), false);
    drainFixtures();
    var detail = submissions.own(full.submissionId(), first);
    assertThat(detail.verdict()).isEqualTo(Verdict.ACCEPTED);
    assertThat(detail.battleId()).isNull();
    assertThat(detail.purpose()).isEqualTo("PRACTICE");
    assertThat(caseCount(full.submissionId())).isEqualTo(problem.cases().size());
    assertThat(caseCount(run.submissionId())).isLessThan(caseCount(full.submissionId()));
    assertThat(detail.publicResults()).hasSize(caseCount(run.submissionId()));
    assertThat(json.writeValueAsString(detail))
        .doesNotContain("expected_output", "testCaseId", "input_data");
    assertThat(submissions.practiceHistory(version, first)).hasSize(2);
    assertThat(submissions.practiceHistory(version, second)).isEmpty();
    assertThat(count("rating_events")).isZero();
    assertThat(count("battles")).isZero();
  }

  @Test
  void verificationIsHashedSingleUseAndResendInvalidatesPreviousLink() {
    String previous = latestToken(first, "VERIFY_EMAIL");
    recovery.requestVerification(first);
    String replacement = latestToken(first, "VERIFY_EMAIL");
    assertThat(replacement).isNotEqualTo(previous);
    assertThatThrownBy(() -> recovery.verify(previous))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
    recovery.verify(replacement);
    assertThat(accountData.account(first).emailVerified()).isTrue();
    assertThatThrownBy(() -> recovery.verify(replacement))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
    assertThat(
            jdbc.sql("SELECT token_hash FROM auth_tokens WHERE user_id=:user")
                .param("user", first)
                .query(String.class)
                .list())
        .allSatisfy(hash -> assertThat(hash).hasSize(64).doesNotContain(replacement));
  }

  @Test
  void expiredAndWrongPurposeRecoveryLinksCannotBeConsumed() {
    String verification = latestToken(first, "VERIFY_EMAIL");
    assertThatThrownBy(() -> recovery.reset(verification, "new-integration-password"))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
    jdbc.sql(
            "UPDATE auth_tokens SET expires_at=clock_timestamp()-interval '1 second' WHERE user_id=:user")
        .param("user", first)
        .update();
    assertThatThrownBy(() -> recovery.verify(verification))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
  }

  @Test
  void passwordResetRevokesEveryPersistedSessionAndCannotBeReplayed() throws Exception {
    Cookie one = login("first", PASSWORD);
    Cookie two = login("first", PASSWORD);
    assertThat(
            jdbc.sql("SELECT count(*) FROM spring_session WHERE principal_name='first'")
                .query(Integer.class)
                .single())
        .isEqualTo(2);
    recovery.requestReset("first@example.invalid");
    String token = latestToken(first, "RESET_PASSWORD");
    recovery.reset(token, "replacement-password-2026");
    http.perform(get("/api/v1/auth/me").cookie(one)).andExpect(status().isUnauthorized());
    http.perform(get("/api/v1/auth/me").cookie(two)).andExpect(status().isUnauthorized());
    assertThatThrownBy(() -> recovery.reset(token, PASSWORD))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
    http.perform(get("/api/v1/auth/me").cookie(login("first", "replacement-password-2026")))
        .andExpect(status().isOk());
  }

  @Test
  void unknownResetAddressHasSamePublicResponseAndCreatesNoOutboxMessage() throws Exception {
    int before = count("mail_outbox");
    var unknown =
        http.perform(
                post("/api/v1/auth/forgot-password")
                    .with(csrf())
                    .contentType("application/json")
                    .content("{\"email\":\"unknown@example.invalid\"}"))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(count("mail_outbox")).isEqualTo(before);
    var known =
        http.perform(
                post("/api/v1/auth/forgot-password")
                    .with(csrf())
                    .contentType("application/json")
                    .content("{\"email\":\"first@example.invalid\"}"))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(unknown.getResponse().getContentAsString())
        .isEqualTo(known.getResponse().getContentAsString());
  }

  @Test
  void accountDeletionCancelsPendingPracticeErasesSourceAndPreservesRatingLedger()
      throws Exception {
    Cookie cookie = login("first", PASSWORD);
    UUID battle = ranked();
    var official = submit(battle, first);
    verdict(official.submissionId(), "ACCEPTED");
    finalizer.attempt(battle);
    UUID version = queries.snapshot(battle, first).problem().versionId();
    var pending = submissions.practice(version, first, "JAVA_21", source(battle), false);
    accountData.delete(first, PASSWORD);
    assertThat(count("rating_events")).isEqualTo(2);
    assertThat(queries.snapshot(battle, second).winnerId()).isEqualTo(first);
    assertThat(
            jdbc.sql("SELECT source_code FROM submissions WHERE user_id=:user")
                .param("user", first)
                .query(String.class)
                .list())
        .allSatisfy(code -> assertThat(code).isEqualTo("// Source removed by retention policy."));
    assertThat(submissions.own(pending.submissionId(), first).verdict())
        .isEqualTo(Verdict.JUDGE_ERROR);
    assertThat(
            jdbc.sql("SELECT status FROM judge_jobs WHERE submission_id=:id")
                .param("id", pending.submissionId())
                .query(String.class)
                .single())
        .isEqualTo("CANCELLED");
    http.perform(get("/api/v1/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
    assertThat(
            jdbc.sql("SELECT count(*) FROM auth_tokens WHERE user_id=:user")
                .param("user", first)
                .query(Integer.class)
                .single())
        .isZero();
    assertThat(
            jdbc.sql("SELECT email FROM users WHERE id=:user")
                .param("user", first)
                .query(String.class)
                .single())
        .endsWith("@deleted.invalid");
  }

  @Test
  void accountDeletionRequiresPasswordAndNoActiveBattleButCancelsAnIdleQueue() {
    matchmaking.enter(first);
    assertThatThrownBy(() -> accountData.delete(first, "incorrect-password-2026"))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
    assertThat(matchmaking.state(first).status()).isEqualTo("QUEUED");
    matchmaking.enter(second);
    matchmaking.pair();
    assertThatThrownBy(() -> accountData.delete(first, PASSWORD))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
    battles.forfeit(first, matchmaking.state(first).battleId());
    matchmaking.enter(first);
    accountData.delete(first, PASSWORD);
    assertThat(count("active_sessions")).isZero();
    assertThat(count("matchmaking_entries")).isZero();
  }

  @Test
  void expiredSourceRedactionIsOneWayAndLeavesRecentAndPendingSourceIntact() {
    UUID version = jdbc.sql("SELECT id FROM problem_versions LIMIT 1").query(UUID.class).single();
    UUID old = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO submissions(id,user_id,problem_version_id,language,source_code,received_at,receive_sequence,is_official,purpose,verdict,judged_at) VALUES (:id,:user,:version,'JAVA_21','historical source',clock_timestamp()-interval '91 days',nextval('practice_receive_sequence'),false,'PRACTICE','ACCEPTED',clock_timestamp()-interval '91 days')")
        .param("id", old)
        .param("user", first)
        .param("version", version)
        .update();
    var recent =
        submissions.practice(version, first, "JAVA_21", ProblemCatalog.WRONG_SOURCE, false);
    assertThat(accountData.purgeExpiredSources()).isEqualTo(1);
    assertThat(accountData.purgeExpiredSources()).isZero();
    assertThat(submissions.own(old, first).sourceCode()).contains("Source removed");
    assertThat(submissions.own(recent.submissionId(), first).sourceCode())
        .isEqualTo(ProblemCatalog.WRONG_SOURCE);
    assertThatThrownBy(
            () ->
                jdbc.sql(
                        "UPDATE submissions SET source_code='restored',source_deleted_at=NULL WHERE id=:id")
                    .param("id", old)
                    .update())
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void mailLeaseFencingPreventsStaleDeliveryFromOverwritingNewerAttempt() {
    var stale = mail.claim().orElseThrow();
    jdbc.sql(
            "UPDATE mail_outbox SET lease_until=clock_timestamp()-interval '1 second' WHERE id=:id")
        .param("id", stale.id())
        .update();
    var current = mail.claim().orElseThrow();
    assertThat(current.id()).isEqualTo(stale.id());
    mail.finish(stale, true);
    assertThat(
            jdbc.sql("SELECT status FROM mail_outbox WHERE id=:id")
                .param("id", current.id())
                .query(String.class)
                .single())
        .isEqualTo("SENDING");
    mail.finish(current, true);
    assertThat(
            jdbc.sql("SELECT body FROM mail_outbox WHERE id=:id")
                .param("id", current.id())
                .query(String.class)
                .single())
        .isEmpty();
  }

  @Test
  void rematchIsPrivateIdempotentAndRequiresBothPlayersConsent() throws Exception {
    UUID original = ranked();
    battles.forfeit(first, original);
    var proposals =
        together(() -> battles.rematch(original, first), () -> battles.rematch(original, first));
    UUID rematch = proposals.getFirst().battleId();
    assertThat(proposals.get(1).battleId()).isEqualTo(rematch);
    assertThat(queries.snapshot(rematch, first).players()).hasSize(1);
    assertThat(matchmaking.state(second).status()).isEqualTo("IDLE");
    assertThat(battles.rematch(original, second).battleId()).isEqualTo(rematch);
    assertThat(queries.snapshot(rematch, second).players()).hasSize(2);
    assertThat(queries.snapshot(rematch, second).mode()).isEqualTo("PRIVATE");
    assertThat(count("battles")).isEqualTo(2);
    assertThat(count("rating_events")).isEqualTo(2);
  }

  @Test
  void administratorRoutesRejectOrdinaryUsersAndFixturePublishingIsForbidden() throws Exception {
    var principal = new AccountPrincipal(first, "first", "", "USER");
    http.perform(get("/api/v1/admin/problems").with(user(principal)))
        .andExpect(status().isForbidden());
    UUID published = jdbc.sql("SELECT id FROM problem_versions LIMIT 1").query(UUID.class).single();
    UUID draft = administration.cloneDraft(published, first);
    var content = administration.detail(draft).content();
    administration.save(draft, first, content);
    assertThatThrownBy(() -> administration.review(draft, first))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
    jdbc.sql("UPDATE problem_versions SET status='REVIEW' WHERE id=:id")
        .param("id", draft)
        .update();
    assertThatThrownBy(() -> administration.save(draft, first, content))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
    assertThatThrownBy(
            () ->
                jdbc.sql(
                        "UPDATE test_cases SET expected_output='altered' WHERE problem_version_id=:id")
                    .param("id", draft)
                    .update())
        .isInstanceOf(DataIntegrityViolationException.class);
    var receipt = submissions.verifyReference(draft, first, content.referenceSource());
    drainFixtures();
    assertThat(submissions.own(receipt.submissionId(), first).verdict())
        .isEqualTo(Verdict.ACCEPTED);
    assertThatThrownBy(() -> administration.publish(draft, first))
        .isInstanceOf(dev.codeduel.common.ApiException.class);
    assertThat(administration.detail(draft).status()).isEqualTo("REVIEW");
  }

  private void drainFixtures() {
    var worker = new JudgeWorker(jobs, gateway, metrics);
    for (int i = 0; i < 400; i++) {
      if (jdbc.sql("SELECT count(*) FROM judge_jobs WHERE status IN ('PENDING','RUNNING')")
              .query(Integer.class)
              .single()
          == 0) return;
      jdbc.sql("UPDATE judge_jobs SET next_retry_at=clock_timestamp() WHERE status='PENDING'")
          .update();
      worker.tick();
    }
    fail("Fixture jobs did not reach a terminal state");
  }

  private int caseCount(UUID submission) {
    return jdbc.sql("SELECT count(*) FROM submission_case_results WHERE submission_id=:id")
        .param("id", submission)
        .query(Integer.class)
        .single();
  }

  private String latestToken(UUID user, String purpose) {
    String subject =
        purpose.equals("VERIFY_EMAIL")
            ? "Verify your CodeDuel email"
            : "Reset your CodeDuel password";
    String body =
        jdbc.sql(
                "SELECT body FROM mail_outbox WHERE user_id=:user AND subject=:subject ORDER BY created_at DESC LIMIT 1")
            .param("user", user)
            .param("subject", subject)
            .query(String.class)
            .single();
    var match = java.util.regex.Pattern.compile("#token=([A-Za-z0-9_-]{43})").matcher(body);
    assertThat(match.find()).isTrue();
    return match.group(1);
  }

  private Cookie login(String username, String password) throws Exception {
    var response =
        http.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn().getResponse();
    var token = json.readTree(response.getContentAsString());
    var logged =
        http.perform(
                post("/api/v1/auth/login")
                    .cookie(response.getCookies()[0])
                    .header(token.get("headerName").asText(), token.get("token").asText())
                    .contentType("application/json")
                    .content(
                        json.writeValueAsString(
                            Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn();
    return logged.getResponse().getCookies()[0];
  }

  private UUID ranked() {
    matchmaking.enter(first);
    matchmaking.enter(second);
    matchmaking.pair();
    UUID id = matchmaking.state(first).battleId();
    battles.ready(first, id);
    battles.ready(second, id);
    return id;
  }

  private String source(UUID battle) {
    var problem = queries.snapshot(battle, first).problem();
    return catalog.find(problem.slug(), problem.version()).orElseThrow().referenceSource();
  }

  private SubmissionService.Receipt submit(UUID id, UUID user) {
    return submissions.submit(id, user, "JAVA_21", source(id), true);
  }

  private void verdict(UUID submission, String verdict) {
    jdbc.sql(
            "UPDATE submissions SET verdict = :verdict, judged_at = clock_timestamp() WHERE id = :id")
        .param("verdict", verdict)
        .param("id", submission)
        .update();
  }

  private void expire(UUID battle) {
    jdbc.sql(
            "UPDATE battles SET starts_at = clock_timestamp() - interval '901 seconds', ends_at = clock_timestamp() - interval '1 second' WHERE id = :id")
        .param("id", battle)
        .update();
  }

  private int count(String table) {
    return jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single();
  }

  private static <T> List<T> together(Callable<T> one, Callable<T> two) throws Exception {
    try (var executor = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      Future<T> a =
          executor.submit(
              () -> {
                start.await();
                return one.call();
              });
      Future<T> b =
          executor.submit(
              () -> {
                start.await();
                return two.call();
              });
      start.countDown();
      return java.util.Arrays.asList(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
    }
  }
}
