package dev.codeduel.submissions;

public enum Verdict {
  PENDING,
  ACCEPTED,
  WRONG_ANSWER,
  TIME_LIMIT_EXCEEDED,
  MEMORY_LIMIT_EXCEEDED,
  RUNTIME_ERROR,
  COMPILATION_ERROR,
  JUDGE_ERROR;

  public boolean terminal() {
    return this != PENDING;
  }
}
