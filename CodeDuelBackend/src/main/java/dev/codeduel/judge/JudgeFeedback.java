package dev.codeduel.judge;

/** Program output stays hostile text; discard terminal controls and bound persisted feedback. */
public final class JudgeFeedback {
  private JudgeFeedback() {}

  public static String safe(String value) {
    if (value == null) return "";
    String plain =
        value
            .replaceAll("\u001B\\[[0-?]*[ -/]*[@-~]", "")
            .replaceAll("[\\p{Cc}\\p{Cf}&&[^\\n\\t]]", "");
    return plain.substring(0, Math.min(8192, plain.length()));
  }
}
