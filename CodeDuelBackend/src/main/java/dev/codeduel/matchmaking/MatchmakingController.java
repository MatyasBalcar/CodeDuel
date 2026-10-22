package dev.codeduel.matchmaking;

import dev.codeduel.auth.AccountPrincipal;
import dev.codeduel.common.RateLimiter;
import java.time.Duration;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matchmaking")
public class MatchmakingController {
  private final MatchmakingService matchmaking;
  private final RateLimiter limits;

  public MatchmakingController(MatchmakingService matchmaking, RateLimiter limits) {
    this.matchmaking = matchmaking;
    this.limits = limits;
  }

  @GetMapping
  MatchmakingService.State state(@AuthenticationPrincipal AccountPrincipal user) {
    return matchmaking.state(user.id());
  }

  @PostMapping("/queue")
  MatchmakingService.State queue(@AuthenticationPrincipal AccountPrincipal user) {
    limits.check("queue:" + user.id(), 30, Duration.ofMinutes(1));
    return matchmaking.enter(user.id());
  }

  @DeleteMapping("/queue")
  MatchmakingService.State cancel(@AuthenticationPrincipal AccountPrincipal user) {
    return matchmaking.cancel(user.id());
  }
}
