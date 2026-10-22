package dev.codeduel.judge;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("codeduel.judge")
public record JudgeSettings(
    Mode mode, String baseUrl, String authToken, int javaLanguageId, String javaLanguageName) {
  public enum Mode {
    DISABLED,
    FIXTURE,
    GO_JUDGE,
    JUDGE0
  }
}
