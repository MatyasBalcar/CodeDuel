package dev.codeduel.judge;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.codeduel.submissions.Verdict;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Real untrusted programs go exclusively to the separately provisioned Linux execution VM. */
@EnabledIfEnvironmentVariable(named = "CODEDUEL_EXECUTOR_URL", matches = ".+")
@Timeout(45)
class SandboxIT {
  static GoJudgeGateway gateway;

  @BeforeAll
  static void connect() {
    gateway =
        new GoJudgeGateway(
            new JudgeSettings(
                JudgeSettings.Mode.GO_JUDGE,
                System.getenv("CODEDUEL_EXECUTOR_URL"),
                System.getenv("CODEDUEL_EXECUTOR_TOKEN"),
                0,
                ""),
            new ObjectMapper());
    gateway.verifyRuntime();
  }

  @Test
  void java21ReadsAndSolvesRealInput() {
    assertThat(
            evaluate(
                    "var in = new java.util.Scanner(System.in); System.out.print(in.nextLong() + in.nextLong());",
                    "2000000000 2000000001",
                    "4000000001")
                .verdict())
        .isEqualTo(Verdict.ACCEPTED);
  }

  @Test
  void incorrectProgramIsActuallyRejected() {
    assertThat(evaluate("System.out.print(42);", "", "43").verdict())
        .isEqualTo(Verdict.WRONG_ANSWER);
  }

  @Test
  void compilerErrorsHaveBoundedUsefulFeedback() {
    var result = evaluate("this does not compile", "PRIVATE_INPUT", "");
    assertThat(result.verdict()).isEqualTo(Verdict.COMPILATION_ERROR);
    assertThat(result.diagnostic())
        .contains("Main.java")
        .hasSizeLessThanOrEqualTo(8192)
        .doesNotContain("PRIVATE_INPUT");
  }

  @Test
  void runtimeErrorsCannotLeakHiddenStderr() {
    var result =
        evaluate(
            "System.err.print(new String(System.in.readAllBytes())); throw new IllegalStateException();",
            "PRIVATE_INPUT",
            "");
    assertThat(result.verdict()).isEqualTo(Verdict.RUNTIME_ERROR);
    assertThat(result.diagnostic()).doesNotContain("PRIVATE_INPUT");
  }

  @Test
  void infiniteLoopIsTerminated() {
    long start = System.nanoTime();
    assertThat(evaluate("while (true) {}", "", "").verdict())
        .isEqualTo(Verdict.TIME_LIMIT_EXCEEDED);
    assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(15));
  }

  @Test
  void memoryBombIsTerminated() {
    assertThat(
            evaluate(
                    "var items = new java.util.ArrayList<byte[]>(); while (true) items.add(new byte[1024 * 1024]);",
                    "",
                    "")
                .verdict())
        .isEqualTo(Verdict.MEMORY_LIMIT_EXCEEDED);
  }

  @Test
  void outputBombCannotBecomeAnInfrastructureFailure() {
    var result = evaluate("while (true) System.out.println(\"x\".repeat(1024));", "", "");
    assertThat(result.verdict()).isIn(Verdict.RUNTIME_ERROR, Verdict.TIME_LIMIT_EXCEEDED);
    assertThat(result.stdout()).hasSizeLessThanOrEqualTo(8192);
  }

  @Test
  void networkDatabaseAndMetadataConnectionsAreBlocked() {
    String body =
        "for (String host : new String[]{\"1.1.1.1\", \"127.0.0.1\", \"192.168.5.2\", \"169.254.169.254\"}) { try (var s = new java.net.Socket()) { s.connect(new java.net.InetSocketAddress(host, 5432), 250); System.out.print(\"CONNECTED \"); } catch (java.io.IOException e) { System.out.print(\"BLOCKED \"); } }";
    assertThat(evaluate(body, "", "BLOCKED BLOCKED BLOCKED BLOCKED").verdict())
        .isEqualTo(Verdict.ACCEPTED);
  }

  @Test
  void hostFilesSecretsAndOtherProcessesAreInvisible() {
    String body =
        "for (String p : new String[]{\"/Users\", \"/etc/shadow\", \"/etc/codeduel-judge.env\", \"/var/run/docker.sock\", \"/opt/codeduel-judge\"}) System.out.print(java.nio.file.Files.exists(java.nio.file.Path.of(p)) + \" \"); System.out.print(System.getenv(\"ES_AUTH_TOKEN\"));";
    assertThat(evaluate(body, "", "false false false false false null").verdict())
        .isEqualTo(Verdict.ACCEPTED);
  }

  @Test
  void sandboxWritableStateDoesNotSurviveAnotherExecution() {
    assertThat(
            evaluate(
                    "java.nio.file.Files.writeString(java.nio.file.Path.of(\"/tmp/private-marker\"), \"secret\");",
                    "",
                    "")
                .verdict())
        .isEqualTo(Verdict.ACCEPTED);
    assertThat(
            evaluate(
                    "System.out.print(java.nio.file.Files.exists(java.nio.file.Path.of(\"/tmp/private-marker\")));",
                    "",
                    "false")
                .verdict())
        .isEqualTo(Verdict.ACCEPTED);
  }

  @Test
  void threadBombIsBoundedAndNextExecutionStillWorks() {
    var result =
        evaluate(
            "while (true) { new Thread(() -> { try { Thread.sleep(60000); } catch (Exception ignored) {} }).start(); }",
            "",
            "");
    assertThat(result.verdict())
        .isIn(Verdict.RUNTIME_ERROR, Verdict.MEMORY_LIMIT_EXCEEDED, Verdict.TIME_LIMIT_EXCEEDED);
    assertThat(evaluate("System.out.print(7);", "", "7").verdict()).isEqualTo(Verdict.ACCEPTED);
  }

  @Test
  @Timeout(180)
  void largestSeededInputOfEveryProblemPassesItsConfiguredSandboxLimits() throws Exception {
    var catalog = new dev.codeduel.problems.ProblemCatalog(new ObjectMapper());
    for (var problem : catalog.all()) {
      var largest =
          problem.cases().stream()
              .max(java.util.Comparator.comparingInt(test -> test.input().length()))
              .orElseThrow();
      var request =
          new JudgeGateway.Request(
              problem.referenceSource(),
              largest.input(),
              problem.timeLimitMs(),
              problem.memoryLimitMb());
      var compiled = gateway.evaluate(request, largest.output(), null);
      try {
        var result =
            compiled.result().verdict().terminal()
                ? compiled.result()
                : gateway.evaluate(request, largest.output(), compiled.executorToken()).result();
        assertThat(result.verdict())
            .as("%s largest input", problem.slug())
            .isEqualTo(Verdict.ACCEPTED);
      } finally {
        gateway.release(compiled.executorToken());
      }
    }
  }

  private JudgeGateway.Result evaluate(String body, String input, String expected) {
    String source =
        "public class Main { public static void main(String[] args) throws Exception { "
            + body
            + " } }";
    var request = new JudgeGateway.Request(source, input, 1000, 256);
    var step = gateway.evaluate(request, expected, null);
    try {
      return step.result().verdict().terminal()
          ? step.result()
          : gateway.evaluate(request, expected, step.executorToken()).result();
    } finally {
      gateway.release(step.executorToken());
    }
  }
}
