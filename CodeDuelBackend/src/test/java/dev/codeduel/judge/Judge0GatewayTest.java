package dev.codeduel.judge;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.codeduel.submissions.Verdict;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Judge0GatewayTest {
  private HttpServer server;
  private java.util.concurrent.ExecutorService executor;
  private final ObjectMapper json = new ObjectMapper();
  private final AtomicReference<String> response = new AtomicReference<>();
  private final AtomicReference<String> sent = new AtomicReference<>();
  private Judge0Gateway gateway;
  private static final String TOKEN = UUID.randomUUID().toString();

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    executor = Executors.newVirtualThreadPerTaskExecutor();
    server.setExecutor(executor);
    server.createContext(
        "/",
        exchange -> {
          sent.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] bytes = response.get().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (var output = exchange.getResponseBody()) {
            output.write(bytes);
          }
        });
    server.start();
    gateway =
        new Judge0Gateway(
            new JudgeSettings(
                JudgeSettings.Mode.JUDGE0,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-only-key",
                91,
                "Java (OpenJDK 21)"),
            json);
  }

  @AfterEach
  void stop() {
    server.stop(0);
    executor.shutdownNow();
  }

  @Test
  void sendsOnlyBoundedServerOwnedSettingsAndBase64Payloads() throws Exception {
    response.set("{\"token\":\"" + TOKEN + "\"}");
    assertThat(
            gateway.start(
                new JudgeGateway.Request("public class Main {}", "hidden input", 2000, 256)))
        .isEqualTo(TOKEN);
    var payload = json.readTree(sent.get());
    assertThat(payload.path("enable_network").asBoolean()).isFalse();
    assertThat(payload.path("memory_limit").asInt()).isEqualTo(262144);
    assertThat(payload.path("cpu_time_limit").asDouble()).isEqualTo(2);
    assertThat(payload.has("compiler_options")).isFalse();
    assertThat(payload.has("callback_url")).isFalse();
    assertThat(
            new String(
                Base64.getDecoder().decode(payload.path("stdin").asText()), StandardCharsets.UTF_8))
        .isEqualTo("hidden input");
  }

  @Test
  void comparesCaseSensitiveTokensInsteadOfJudgeCompletionOrRuntime() {
    output("  4\t8\n");
    assertThat(gateway.poll(TOKEN, "4 8").verdict()).isEqualTo(Verdict.ACCEPTED);
    output("balanced");
    assertThat(gateway.poll(TOKEN, "BALANCED").verdict()).isEqualTo(Verdict.WRONG_ANSWER);
  }

  @Test
  void pendingAndInfrastructureFailuresAreNotPlayerDefeats() {
    response.set("{\"status\":{\"id\":2}}");
    assertThat(gateway.poll(TOKEN, "answer").verdict()).isEqualTo(Verdict.PENDING);
    response.set("{\"status\":{\"id\":13},\"message\":\"private diagnostic\"}");
    assertThatThrownBy(() -> gateway.poll(TOKEN, "answer"))
        .isInstanceOf(JudgeUnavailableException.class)
        .hasMessage("Execution service unavailable");
  }

  @Test
  void validatesTheActualCompilerIdentity() {
    response.set("{\"name\":\"Java (OpenJDK 13.0.1)\"}");
    assertThatIllegalStateException().isThrownBy(gateway::verifyRuntime);
    response.set("{\"name\":\"Java (OpenJDK 21)\"}");
    gateway.verifyRuntime();
  }

  @Test
  void rejectsHostileOrOversizedResponsesBeforePersistingFeedback() {
    response.set("x".repeat(140000));
    assertThatThrownBy(() -> gateway.poll(TOKEN, "answer"))
        .isInstanceOf(JudgeUnavailableException.class);
    response.set("{\"status\":{\"id\":3},\"stdout\":\"not-base64!\"}");
    assertThatThrownBy(() -> gateway.poll(TOKEN, "answer"))
        .isInstanceOf(JudgeUnavailableException.class);
  }

  @Test
  void cannotInjectExecutorLimitsOrAnArbitraryTokenPath() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> gateway.start(new JudgeGateway.Request("source", "input", 999999, 256)));
    assertThatThrownBy(() -> gateway.poll("../../secrets", "answer"))
        .isInstanceOf(JudgeUnavailableException.class);
  }

  private void output(String value) {
    response.set(
        "{\"status\":{\"id\":3},\"time\":\"0.015\",\"memory\":123,\"stdout\":\""
            + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8))
            + "\"}");
  }
}
