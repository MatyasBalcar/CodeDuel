package dev.codeduel.judge;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JudgeOutputTest {
  @Test
  void comparesReturnedArraysAndNumbersAsValues() {
    assertThat(JudgeOutput.matches("[1, [2,3]]\n", "[1.0,[2,3.0]]", true)).isTrue();
    assertThat(JudgeOutput.matches("9223372036854775807", "9223372036854775807", true)).isTrue();
    assertThat(JudgeOutput.matches("9223372036854775806", "9223372036854775807", true)).isFalse();
    assertThat(JudgeOutput.matches("[2,1]", "[1,2]", true)).isFalse();
    assertThat(JudgeOutput.matches("[1]", "[1,2]", true)).isFalse();
    assertThat(JudgeOutput.matches("true", "1", true)).isFalse();
  }

  @Test
  void preservesWhitespaceInsideReturnedStringsAndRejectsExtraOutput() {
    assertThat(JudgeOutput.matches("\"a  b\"", "\"a b\"", true)).isFalse();
    assertThat(JudgeOutput.matches("\"\"", "\"\"", true)).isTrue();
    assertThat(JudgeOutput.matches("true false", "true", true)).isFalse();
    assertThat(JudgeOutput.matches("debug\n42", "42", true)).isFalse();
    assertThat(JudgeOutput.matches("", "0", true)).isFalse();
    assertThat(JudgeOutput.matches("\"YES\"", "true", true)).isFalse();
  }

  @Test
  void keepsLegacyTokenComparisonForHistoricVersions() {
    assertThat(JudgeOutput.matches("1\t2\n", "1 2", false)).isTrue();
    assertThat(JudgeOutput.matches("yes", "YES", false)).isFalse();
  }
}
