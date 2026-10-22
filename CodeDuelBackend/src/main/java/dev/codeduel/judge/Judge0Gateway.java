package dev.codeduel.judge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.codeduel.submissions.Verdict;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Narrow Judge0 client: no caller-controlled commands, callbacks, compiler flags or limits. */
public final class Judge0Gateway implements JudgeGateway {
  private static final int MAX_RESPONSE_BYTES = 131072;
  private final JudgeSettings settings;
  private final ObjectMapper json;
  private final HttpClient http;
  private final String baseUrl;

  public Judge0Gateway(JudgeSettings settings, ObjectMapper json) {
    this.settings = settings;
    this.json = json;
    URI uri = URI.create(settings.baseUrl());
    if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null
        || settings.javaLanguageId() <= 0
        || !settings.javaLanguageName().matches("Java \\(.*\\b21\\b.*\\)")) {
      throw new IllegalArgumentException(
          "Configure an internal Judge0 endpoint and a pinned Java 21 language");
    }
    baseUrl = settings.baseUrl().replaceAll("/+$", "");
    http =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
  }

  public void verifyRuntime() {
    JsonNode runtime = exchange("GET", "/languages/" + settings.javaLanguageId(), null);
    if (!settings.javaLanguageName().equals(runtime.path("name").asText())) {
      throw new IllegalStateException(
          "Judge0 language does not match the configured Java 21 runtime");
    }
  }

  @Override
  public String kind() {
    return "JUDGE0";
  }

  @Override
  public Step evaluate(Request request, String expectedOutput, String token) {
    if (token == null) return new Step(start(request), Result.pending());
    return new Step(token, poll(token, expectedOutput));
  }

  public String start(Request request) {
    if (request.timeLimitMs() < 100
        || request.timeLimitMs() > 10000
        || request.memoryLimitMb() < 64
        || request.memoryLimitMb() > 512) {
      throw new IllegalArgumentException("Execution limits outside policy");
    }
    var body = new LinkedHashMap<String, Object>();
    body.put("language_id", settings.javaLanguageId());
    body.put("source_code", encode(request.source()));
    body.put("stdin", encode(request.input()));
    body.put("cpu_time_limit", request.timeLimitMs() / 1000.0);
    body.put("wall_time_limit", Math.min(20, request.timeLimitMs() / 1000.0 + 5));
    body.put("memory_limit", request.memoryLimitMb() * 1024);
    body.put("max_file_size", 64);
    body.put("max_processes_and_or_threads", 64);
    body.put("enable_network", false);
    body.put("redirect_stderr_to_stdout", false);
    String token =
        exchange("POST", "/submissions?base64_encoded=true&wait=false", body)
            .path("token")
            .asText();
    try {
      if (!UUID.fromString(token).toString().equals(token)) throw new IllegalArgumentException();
    } catch (IllegalArgumentException ex) {
      throw new JudgeUnavailableException();
    }
    return token;
  }

  public Result poll(String token, String expectedOutput) {
    try {
      UUID.fromString(token);
    } catch (IllegalArgumentException ex) {
      throw new JudgeUnavailableException();
    }
    JsonNode response =
        exchange(
            "GET",
            "/submissions/"
                + token
                + "?base64_encoded=true&fields=status,stdout,compile_output,time,memory",
            null);
    int status = response.path("status").path("id").asInt();
    if (status == 1 || status == 2) return Result.pending();
    Verdict verdict =
        switch (status) {
          case 3 ->
              canonical(decode(response.path("stdout").asText("")))
                      .equals(canonical(expectedOutput))
                  ? Verdict.ACCEPTED
                  : Verdict.WRONG_ANSWER;
          case 4 -> Verdict.WRONG_ANSWER;
          case 5 -> Verdict.TIME_LIMIT_EXCEEDED;
          case 6 -> Verdict.COMPILATION_ERROR;
          case 7, 8, 9, 10, 11, 12 -> Verdict.RUNTIME_ERROR;
          default -> throw new JudgeUnavailableException();
        };
    double seconds = response.path("time").asDouble(0);
    int memory = response.path("memory").asInt(0);
    if (!Double.isFinite(seconds) || seconds < 0 || seconds > 1000 || memory < 0)
      throw new JudgeUnavailableException();
    return new Result(
        verdict,
        (int) Math.ceil(seconds * 1000),
        memory,
        verdict == Verdict.COMPILATION_ERROR
            ? JudgeFeedback.safe(decode(response.path("compile_output").asText("")))
            : "",
        JudgeFeedback.safe(decode(response.path("stdout").asText(""))));
  }

  static String canonical(String output) {
    return output.strip().replaceAll("\\s+", " ");
  }

  private static String encode(String value) {
    return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String decode(String value) {
    try {
      byte[] decoded = Base64.getDecoder().decode(value);
      if (decoded.length > 65536) throw new JudgeUnavailableException();
      return new String(decoded, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      throw new JudgeUnavailableException();
    }
  }

  private JsonNode exchange(String method, String path, Object payload) {
    try {
      var request =
          HttpRequest.newBuilder(URI.create(baseUrl + path))
              .timeout(Duration.ofSeconds(4))
              .header("Accept", "application/json");
      if (!settings.authToken().isBlank()) request.header("X-Auth-Token", settings.authToken());
      if (payload == null) request.GET();
      else
        request
            .header("Content-Type", "application/json")
            .method(
                method, HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(payload)));
      var pending =
          http.sendAsync(request.build(), info -> new LimitedBodySubscriber(MAX_RESPONSE_BYTES));
      HttpResponse<byte[]> response;
      try {
        response = pending.get(5, TimeUnit.SECONDS);
      } catch (TimeoutException ex) {
        pending.cancel(true);
        throw new JudgeUnavailableException();
      }
      if (response.statusCode() < 200 || response.statusCode() >= 300)
        throw new JudgeUnavailableException();
      JsonNode parsed = json.readTree(response.body());
      if (parsed == null || !parsed.isObject()) throw new JudgeUnavailableException();
      return parsed;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new JudgeUnavailableException();
    } catch (IOException | IllegalArgumentException | ExecutionException ex) {
      throw new JudgeUnavailableException();
    }
  }
}
