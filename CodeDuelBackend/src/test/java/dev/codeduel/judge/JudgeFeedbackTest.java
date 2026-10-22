package dev.codeduel.judge;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JudgeFeedbackTest {
  @Test
  void removesTerminalAndDirectionControlsWithoutRemovingReadableLines() {
    assertThat(JudgeFeedback.safe("\u001b[31merror\u001b[0m\u0000\u202e\n\tline"))
        .isEqualTo("error\n\tline");
  }

  @Test
  void boundsHostileOutputAndHandlesMissingStreams() {
    assertThat(JudgeFeedback.safe("x".repeat(20000))).hasSize(8192);
    assertThat(JudgeFeedback.safe(null)).isEmpty();
  }
}
