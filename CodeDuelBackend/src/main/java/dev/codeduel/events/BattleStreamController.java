package dev.codeduel.events;

import dev.codeduel.auth.AccountPrincipal;
import dev.codeduel.battles.BattleStore;
import dev.codeduel.common.ApiException;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Database-backed SSE replay works across processes without a local-only message broker. */
@RestController
@ConditionalOnWebApplication
public class BattleStreamController {
  private final ConcurrentHashMap<UUID, Connection> streams = new ConcurrentHashMap<>();
  private final BattleStore battles;
  private final BattleEvents events;

  public BattleStreamController(BattleStore battles, BattleEvents events) {
    this.battles = battles;
    this.events = events;
  }

  @GetMapping(value = "/api/v1/battles/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public synchronized SseEmitter connect(
      @AuthenticationPrincipal AccountPrincipal user,
      @PathVariable UUID id,
      @RequestParam(defaultValue = "0") long after,
      @RequestHeader(value = "Last-Event-ID", required = false) String lastId) {
    battles.requireMember(id, user.id());
    if (streams.size() >= 200
        || streams.values().stream().filter(c -> c.userId.equals(user.id())).count() >= 3) {
      throw new ApiException(
          HttpStatus.TOO_MANY_REQUESTS, "STREAM_LIMIT", "Too many open live connections.");
    }
    if (lastId != null) {
      try {
        after = Math.max(after, Long.parseLong(lastId));
      } catch (NumberFormatException ex) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "Invalid event cursor.");
      }
    }
    var emitter = new SseEmitter(300000L);
    UUID connectionId = UUID.randomUUID();
    streams.put(connectionId, new Connection(user.id(), id, emitter, Math.max(0, after)));
    emitter.onCompletion(() -> streams.remove(connectionId));
    emitter.onTimeout(() -> streams.remove(connectionId));
    emitter.onError(error -> streams.remove(connectionId));
    return emitter;
  }

  @Scheduled(fixedDelay = 1000)
  public void deliver() {
    for (var entry : streams.entrySet()) {
      var stream = entry.getValue();
      try {
        for (var event : events.since(stream.battleId, stream.userId, stream.cursor)) {
          stream.emitter.send(
              SseEmitter.event().id(Long.toString(event.sequence())).name("battle").data(event));
          stream.cursor = event.sequence();
        }
        stream.emitter.send(SseEmitter.event().comment("heartbeat"));
      } catch (IOException | RuntimeException ex) {
        streams.remove(entry.getKey());
        stream.emitter.complete();
      }
    }
  }

  private static final class Connection {
    final UUID userId;
    final UUID battleId;
    final SseEmitter emitter;
    volatile long cursor;

    Connection(UUID userId, UUID battleId, SseEmitter emitter, long cursor) {
      this.userId = userId;
      this.battleId = battleId;
      this.emitter = emitter;
      this.cursor = cursor;
    }
  }
}
