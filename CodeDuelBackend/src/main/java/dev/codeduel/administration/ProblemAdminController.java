package dev.codeduel.administration;

import dev.codeduel.auth.AccountPrincipal;
import dev.codeduel.submissions.SubmissionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/problems")
public class ProblemAdminController {
  private final ProblemAdministration problems;

  public ProblemAdminController(ProblemAdministration problems) {
    this.problems = problems;
  }

  @GetMapping
  List<ProblemAdministration.VersionSummary> list() {
    return problems.list();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  Map<String, UUID> create(
      @Valid @RequestBody ProblemAdministration.Draft body,
      @AuthenticationPrincipal AccountPrincipal user) {
    return Map.of("versionId", problems.create(user.id(), body));
  }

  @GetMapping("/versions/{id}")
  ProblemAdministration.VersionDetail detail(@PathVariable UUID id) {
    return problems.detail(id);
  }

  @PutMapping("/versions/{id}")
  void save(
      @PathVariable UUID id,
      @Valid @RequestBody ProblemAdministration.Draft body,
      @AuthenticationPrincipal AccountPrincipal user) {
    problems.save(id, user.id(), body);
  }

  @PostMapping("/versions/{id}/clone")
  Map<String, UUID> clone(@PathVariable UUID id, @AuthenticationPrincipal AccountPrincipal user) {
    return Map.of("versionId", problems.cloneDraft(id, user.id()));
  }

  @PostMapping("/versions/{id}/review")
  SubmissionService.Receipt review(
      @PathVariable UUID id, @AuthenticationPrincipal AccountPrincipal user) {
    return problems.review(id, user.id());
  }

  @PostMapping("/versions/{id}/publish")
  void publish(@PathVariable UUID id, @AuthenticationPrincipal AccountPrincipal user) {
    problems.publish(id, user.id());
  }

  @PostMapping("/{id}/retire")
  void retire(@PathVariable UUID id, @AuthenticationPrincipal AccountPrincipal user) {
    problems.active(id, user.id(), false);
  }

  @PostMapping("/{id}/activate")
  void activate(@PathVariable UUID id, @AuthenticationPrincipal AccountPrincipal user) {
    problems.active(id, user.id(), true);
  }
}
