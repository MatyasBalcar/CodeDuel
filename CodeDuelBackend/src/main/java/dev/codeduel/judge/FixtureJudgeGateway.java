package dev.codeduel.judge;

import dev.codeduel.problems.ProblemCatalog;
import dev.codeduel.submissions.Verdict;
import java.util.Set;
import java.util.stream.Collectors;

/** Exact fixtures only. This class never compiles, runs, or infers correctness of user code. */
final class FixtureJudgeGateway implements JudgeGateway {
  private final Set<String> sources;
  private final Set<String> wrongSources;

  FixtureJudgeGateway(ProblemCatalog catalog) {
    sources =
        catalog.all().stream()
            .map(ProblemCatalog.Content::referenceSource)
            .collect(Collectors.toUnmodifiableSet());
    var wrong = new java.util.HashSet<String>();
    wrong.add(ProblemCatalog.WRONG_SOURCE);
    for (var problem : catalog.all()) wrong.add(ProblemCatalog.wrongSource(problem));
    wrongSources = Set.copyOf(wrong);
  }

  @Override
  public String kind() {
    return "FIXTURE";
  }

  @Override
  public Step evaluate(Request request, String expectedOutput, String token) {
    String checkpoint = token == null ? start(request) : token;
    return new Step(checkpoint, poll(checkpoint, expectedOutput));
  }

  public String start(Request request) {
    if (sources.contains(request.source())) return "fixture:ACCEPTED";
    if (wrongSources.contains(request.source())) return "fixture:WRONG_ANSWER";
    throw new JudgeUnavailableException();
  }

  public Result poll(String token, String expectedOutput) {
    return new Result(Verdict.valueOf(token.substring("fixture:".length())), 0, 0);
  }
}
