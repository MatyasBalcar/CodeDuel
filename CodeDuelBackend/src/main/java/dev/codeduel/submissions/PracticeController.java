package dev.codeduel.submissions;

import dev.codeduel.auth.AccountPrincipal;
import dev.codeduel.common.RateLimiter;
import dev.codeduel.problems.ProblemService;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/practice/{slug}")
public class PracticeController {
  private final SubmissionService submissions;
  private final ProblemService problems;
  private final RateLimiter limits;

  public PracticeController(
      SubmissionService submissions, ProblemService problems, RateLimiter limits) {
    this.submissions = submissions;
    this.problems = problems;
    this.limits = limits;
  }

  @PostMapping({"/submissions", "/run"})
  @ResponseStatus(HttpStatus.ACCEPTED)
  SubmissionService.Receipt submit(
      @PathVariable String slug,
      @Valid @RequestBody SubmissionController.Source body,
      @AuthenticationPrincipal AccountPrincipal user,
      jakarta.servlet.http.HttpServletRequest request) {
    boolean examples = request.getRequestURI().endsWith("/run");
    limits.check(
        (examples ? "run:" : "submit:") + user.id(), examples ? 15 : 10, Duration.ofMinutes(1));
    return submissions.practice(
        problems.published(slug).versionId(),
        user.id(),
        body.language(),
        body.sourceCode(),
        examples);
  }

  @GetMapping("/submissions")
  List<SubmissionService.PracticeEntry> history(
      @PathVariable String slug, @AuthenticationPrincipal AccountPrincipal user) {
    return submissions.practiceHistory(problems.published(slug).versionId(), user.id());
  }
}
