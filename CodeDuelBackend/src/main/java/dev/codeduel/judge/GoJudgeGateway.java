package dev.codeduel.judge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.codeduel.submissions.Verdict;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/** Java-only adapter for go-judge in a dedicated Linux execution VM, never on the API host. */
public final class GoJudgeGateway implements JudgeGateway {
  private static final String COMPILE =
      "mkdir classes && /usr/bin/javac -J-Xmx192m -J-XX:ActiveProcessorCount=1 -J-XX:+UseSerialGC -encoding UTF-8 -proc:none --release 21 -d classes Main.java && /usr/bin/jar -J-Djava.io.tmpdir=/w -J-Xmx128m -J-XX:ActiveProcessorCount=1 -J-XX:+UseSerialGC --create --file program.jar --main-class Main -C classes .";
  private static final List<String> ENV =
      List.of("PATH=/usr/bin:/bin", "HOME=/tmp", "LANG=C.UTF-8", "TZ=UTC");
  private final String baseUrl;
  private final String authToken;
  private final ObjectMapper json;
  private final HttpClient http =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(3))
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();

  public GoJudgeGateway(JudgeSettings settings, ObjectMapper json) {
    URI uri = URI.create(settings.baseUrl());
    if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null
        || !List.of("", "/").contains(uri.getPath())
        || settings.authToken().length() < 32) {
      throw new IllegalArgumentException(
          "Configure a private go-judge endpoint and a strong bearer token");
    }
    baseUrl = settings.baseUrl().replaceAll("/+$", "");
    authToken = settings.authToken();
    this.json = json;
  }

  public void verifyRuntime() {
    var version = exchange("GET", "/version", null);
    if (!version.path("buildVersion").asText().strip().equals("v1.13.3")
        || !"linux".equals(version.path("os").asText())) {
      throw new IllegalStateException("Expected the pinned Linux go-judge v1.13.3 executor");
    }
    var request =
        new Request(
            "public class Main { public static void main(String[] args) { System.out.print(Runtime.version().feature()); } }",
            "",
            2000,
            256);
    var compiled = evaluate(request, "21", null);
    var result =
        compiled.result().verdict().terminal()
            ? compiled.result()
            : evaluate(request, "21", compiled.executorToken()).result();
    release(compiled.executorToken());
    if (result.verdict() != Verdict.ACCEPTED)
      throw new IllegalStateException("The isolated Java 21 runtime self-test failed");
  }

  @Override
  public String kind() {
    return "GO_JUDGE";
  }

  @Override
  public boolean reusableCheckpoint() {
    return true;
  }

  @Override
  public void release(String token) {
    if (token == null) return;
    validateToken(token);
    try {
      exchange("DELETE", "/file/" + token, null);
    } catch (JudgeUnavailableException ignored) {
    }
  }

  @Override
  public Step evaluate(Request request, String expected, String token) {
    if (request.timeLimitMs() < 100
        || request.timeLimitMs() > 10000
        || request.memoryLimitMb() < 128
        || request.memoryLimitMb() > 512) {
      throw new IllegalArgumentException("Execution limits outside Java policy");
    }
    if (token == null) {
      var command = command(List.of("/bin/sh", "-c", COMPILE), "", 10000, 512);
      command.put("clockLimit", 15_000_000_000L);
      command.put(
          "copyIn",
          request.function() == null
              ? Map.of("Main.java", Map.of("content", request.source()))
              : Map.of(
                  "Main.java",
                  Map.of("content", FunctionProgram.runner(request.function())),
                  "Solution.java",
                  Map.of("content", request.source())));
      command.put("copyOutCached", List.of("program.jar"));
      command.put("copyOutMax", 4 * 1024 * 1024);
      var result = run(command);
      String status = result.path("status").asText();
      if (!"Accepted".equals(status)) {
        if ("Internal Error".equals(status) || "File Error".equals(status))
          throw new JudgeUnavailableException();
        String diagnostic =
            result.path("files").path("stderr").asText("")
                + result.path("files").path("stdout").asText("");
        if (diagnostic.isBlank()) diagnostic = "Compilation failed or exceeded its resource limit.";
        return new Step(
            null, new Result(Verdict.COMPILATION_ERROR, 0, 0, JudgeFeedback.safe(diagnostic), ""));
      }
      String fileId = result.path("fileIds").path("program.jar").asText();
      validateToken(fileId);
      return new Step(fileId, Result.pending());
    }
    validateToken(token);
    int heapMb = Math.max(16, request.memoryLimitMb() - 96);
    var command =
        command(
            List.of(
                "/usr/bin/java",
                "-Xms16m",
                "-Xmx" + heapMb + "m",
                "-Xss256k",
                "-XX:ActiveProcessorCount=1",
                "-XX:+UseSerialGC",
                "-XX:MaxMetaspaceSize=48m",
                "-XX:ReservedCodeCacheSize=32m",
                "-XX:+ExitOnOutOfMemoryError",
                "-XX:-UsePerfData",
                "-Dfile.encoding=UTF-8",
                "-jar",
                "program.jar"),
            request.input(),
            request.timeLimitMs(),
            request.memoryLimitMb());
    command.put("copyIn", Map.of("program.jar", Map.of("fileId", token)));
    JsonNode response = run(command);
    String stdout = response.path("files").path("stdout").asText("");
    String stderr = response.path("files").path("stderr").asText("");
    Verdict verdict =
        switch (response.path("status").asText()) {
          case "Accepted" ->
              JudgeOutput.matches(stdout, expected, request.function() != null)
                  ? Verdict.ACCEPTED
                  : Verdict.WRONG_ANSWER;
          case "Time Limit Exceeded" -> Verdict.TIME_LIMIT_EXCEEDED;
          case "Memory Limit Exceeded" -> Verdict.MEMORY_LIMIT_EXCEEDED;
          case "Output Limit Exceeded", "Signalled", "Nonzero Exit Status", "Dangerous Syscall" ->
              stderr.contains("java.lang.OutOfMemoryError")
                      || stdout.contains("java.lang.OutOfMemoryError")
                  ? Verdict.MEMORY_LIMIT_EXCEEDED
                  : Verdict.RUNTIME_ERROR;
          default -> throw new JudgeUnavailableException();
        };
    long nanos = response.path("time").asLong(-1);
    long bytes = response.path("memory").asLong(-1);
    if (nanos < 0 || nanos > 60_000_000_000L || bytes < 0 || bytes > 1024L * 1024 * 1024)
      throw new JudgeUnavailableException();
    return new Step(
        token,
        new Result(
            verdict,
            (int) ((nanos + 999999) / 1_000_000),
            (int) ((bytes + 1023) / 1024),
            "",
            JudgeFeedback.safe(stdout)));
  }

  private Map<String, Object> command(List<String> args, String input, int timeMs, int memoryMb) {
    var cmd = new LinkedHashMap<String, Object>();
    cmd.put("args", args);
    cmd.put("env", ENV);
    cmd.put(
        "files",
        List.of(
            Map.of("content", input),
            Map.of("name", "stdout", "max", 262144),
            Map.of("name", "stderr", "max", 16384)));
    cmd.put("cpuLimit", timeMs * 1_000_000L);
    cmd.put("clockLimit", (timeMs + 3000) * 1_000_000L);
    cmd.put("memoryLimit", memoryMb * 1024L * 1024);
    cmd.put("stackLimit", 32 * 1024 * 1024);
    cmd.put("procLimit", 48);
    return cmd;
  }

  private JsonNode run(Map<String, Object> command) {
    JsonNode response = exchange("POST", "/run", Map.of("cmd", List.of(command)));
    if (!response.isArray() || response.size() != 1 || !response.get(0).isObject())
      throw new JudgeUnavailableException();
    return response.get(0);
  }

  private static void validateToken(String token) {
    if (token == null || !token.matches("[A-Za-z0-9_-]{1,128}"))
      throw new JudgeUnavailableException();
  }

  private JsonNode exchange(String method, String path, Object body) {
    try {
      var builder =
          HttpRequest.newBuilder(URI.create(baseUrl + path))
              .timeout(Duration.ofSeconds(20))
              .header("Accept", "application/json")
              .header("Authorization", "Bearer " + authToken);
      if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
      else
        builder
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body)));
      var future = http.sendAsync(builder.build(), info -> new LimitedBodySubscriber(2097152));
      HttpResponse<byte[]> response;
      try {
        response = future.get(21, TimeUnit.SECONDS);
      } catch (java.util.concurrent.TimeoutException ex) {
        future.cancel(true);
        throw new JudgeUnavailableException();
      }
      if (response.statusCode() < 200 || response.statusCode() >= 300)
        throw new JudgeUnavailableException();
      if ("DELETE".equals(method)) return json.createObjectNode();
      JsonNode parsed = json.readTree(response.body());
      if (parsed == null) throw new JudgeUnavailableException();
      return parsed;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new JudgeUnavailableException();
    } catch (IOException | IllegalArgumentException | ExecutionException ex) {
      throw new JudgeUnavailableException();
    }
  }
}
