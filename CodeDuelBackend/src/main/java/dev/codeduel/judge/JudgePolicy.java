package dev.codeduel.judge;

import dev.codeduel.common.ApiException;
import dev.codeduel.problems.ProblemCatalog;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JudgePolicy {
  private final JudgeSettings settings;
  private final ProblemCatalog catalog;

  public JudgePolicy(JudgeSettings settings, ProblemCatalog catalog) {
    this.settings = settings;
    this.catalog = catalog;
  }

  public void check(String source, String slug) {
    if (settings.mode() == JudgeSettings.Mode.DISABLED)
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "JUDGE_DISABLED",
          "An isolated judge has not been configured.");
    if (settings.mode() == JudgeSettings.Mode.FIXTURE) {
      boolean known =
          ProblemCatalog.WRONG_SOURCE.equals(source)
              || catalog
                  .find(slug)
                  .map(problem -> problem.referenceSource().equals(source))
                  .orElse(false);
      if (!known)
        throw new ApiException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "FIXTURE_ONLY",
            "This demo accepts only the supplied sample programs. Configure an isolated Java judge to evaluate your own code.");
    }
  }
}
