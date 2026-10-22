package dev.codeduel.battles;

import dev.codeduel.auth.AccountPrincipal;
import dev.codeduel.common.RateLimiter;
import dev.codeduel.events.BattleEvents;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/battles")
public class BattleController {
  private final BattleService battles;
  private final BattleQueries queries;
  private final BattleEvents events;
  private final RateLimiter limits;

  public BattleController(
      BattleService battles, BattleQueries queries, BattleEvents events, RateLimiter limits) {
    this.battles = battles;
    this.queries = queries;
    this.events = events;
    this.limits = limits;
  }

  @PostMapping("/private")
  BattleService.PrivateRoom create(@AuthenticationPrincipal AccountPrincipal user) {
    limits.check("rooms:" + user.id(), 10, Duration.ofMinutes(1));
    return battles.createPrivate(user.id());
  }

  @PostMapping("/private/{code}/join")
  Map<String, UUID> join(
      @AuthenticationPrincipal AccountPrincipal user, @PathVariable String code) {
    limits.check("invites:" + user.id(), 20, Duration.ofMinutes(1));
    return Map.of("battleId", battles.joinPrivate(user.id(), code));
  }

  @GetMapping("/{id}")
  BattleQueries.Snapshot snapshot(
      @AuthenticationPrincipal AccountPrincipal user, @PathVariable UUID id) {
    return queries.snapshot(id, user.id());
  }

  @PostMapping("/{id}/ready")
  void ready(@AuthenticationPrincipal AccountPrincipal user, @PathVariable UUID id) {
    battles.ready(user.id(), id);
  }

  @PostMapping("/{id}/forfeit")
  void forfeit(@AuthenticationPrincipal AccountPrincipal user, @PathVariable UUID id) {
    battles.forfeit(user.id(), id);
  }

  @GetMapping("/{id}/rematch")
  Map<String, Object> rematchState(
      @PathVariable UUID id, @AuthenticationPrincipal AccountPrincipal user) {
    var proposal = battles.rematchProposal(id, user.id());
    return proposal == null
        ? Map.of()
        : Map.of("battleId", proposal.battleId(), "proposedBy", proposal.proposedBy());
  }

  @PostMapping("/{id}/rematch")
  BattleService.PrivateRoom rematch(
      @PathVariable UUID id, @AuthenticationPrincipal AccountPrincipal user) {
    limits.check("rematch:" + user.id(), 10, Duration.ofMinutes(1));
    return battles.rematch(id, user.id());
  }

  @GetMapping("/{id}/timeline")
  List<BattleEvents.Event> timeline(
      @AuthenticationPrincipal AccountPrincipal user,
      @PathVariable UUID id,
      @RequestParam(defaultValue = "0") long after) {
    queries.requireFinished(id, user.id());
    return events.since(id, user.id(), after);
  }
}
