package dev.codeduel.judge;

/** Intentionally excludes remote error bodies, output, submitted source and credentials. */
public final class JudgeUnavailableException extends RuntimeException {
  public JudgeUnavailableException() {
    super("Execution service unavailable");
  }
}
