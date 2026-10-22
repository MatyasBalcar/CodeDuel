package dev.codeduel.common;

import dev.codeduel.judge.JudgeSettings;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemController {
  private final JudgeSettings judge;

  public SystemController(JudgeSettings judge) {
    this.judge = judge;
  }

  @GetMapping("/api/v1/system")
  Map<String, Object> info() {
    return Map.of("judgeMode", judge.mode(), "language", "JAVA_21", "version", "0.1.0");
  }
}
