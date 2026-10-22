package dev.codeduel.judge;

import dev.codeduel.submissions.Verdict;

/** The worker sends bounded, server-owned execution settings across this boundary. */
public interface JudgeGateway {
  Step evaluate(Request request, String expectedOutput, String executorToken);

  default String kind() {
    return "UNSPECIFIED";
  }

  default boolean reusableCheckpoint() {
    return false;
  }

  default void release(String executorToken) {}

  /** A durable checkpoint between remote steps; a crash may repeat a step, never its commit. */
  record Step(String executorToken, Result result) {}

  record Request(String source, String input, int timeLimitMs, int memoryLimitMb) {}

  record Result(Verdict verdict, int runtimeMs, int memoryKb, String diagnostic, String stdout) {
    public Result(Verdict verdict, int runtimeMs, int memoryKb) {
      this(verdict, runtimeMs, memoryKb, "", "");
    }

    public static Result pending() {
      return new Result(Verdict.PENDING, 0, 0);
    }
  }
}
