package dev.codeduel.administration;

import dev.codeduel.auth.AccountPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/judge-jobs")
public class OperationsController {
  private final JudgeOperations operations;

  public OperationsController(JudgeOperations operations) {
    this.operations = operations;
  }

  @GetMapping
  List<JudgeOperations.Job> jobs() {
    return operations.jobs();
  }

  @PostMapping("/{id}/release-expired-lease")
  void release(@PathVariable UUID id, @AuthenticationPrincipal AccountPrincipal user) {
    operations.releaseExpired(id, user.id());
  }
}
