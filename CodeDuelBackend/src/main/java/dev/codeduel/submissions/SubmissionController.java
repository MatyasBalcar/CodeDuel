package dev.codeduel.submissions;

import dev.codeduel.auth.AccountPrincipal;
import dev.codeduel.common.RateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SubmissionController {
  private final SubmissionService submissions;
  private final RateLimiter limits;

  public SubmissionController(SubmissionService submissions, RateLimiter limits) {
    this.submissions = submissions;
    this.limits = limits;
  }

  @PostMapping("/battles/{id}/submissions")
  @ResponseStatus(HttpStatus.ACCEPTED)
  SubmissionService.Receipt submit(
      @AuthenticationPrincipal AccountPrincipal user,
      @PathVariable UUID id,
      @Valid @RequestBody Source body) {
    limits.check("submit:" + user.id(), 10, Duration.ofMinutes(1));
    return submissions.submit(id, user.id(), body.language(), body.sourceCode(), true);
  }

  @PostMapping("/battles/{id}/run")
  @ResponseStatus(HttpStatus.ACCEPTED)
  SubmissionService.Receipt run(
      @AuthenticationPrincipal AccountPrincipal user,
      @PathVariable UUID id,
      @Valid @RequestBody Source body) {
    limits.check("run:" + user.id(), 15, Duration.ofMinutes(1));
    return submissions.submit(id, user.id(), body.language(), body.sourceCode(), false);
  }

  @GetMapping("/submissions/{id}")
  SubmissionService.Detail own(
      @AuthenticationPrincipal AccountPrincipal user, @PathVariable UUID id) {
    return submissions.own(id, user.id());
  }

  record Source(
      @NotBlank @Size(max = 16) String language, @NotBlank @Size(max = 65536) String sourceCode) {}
}
