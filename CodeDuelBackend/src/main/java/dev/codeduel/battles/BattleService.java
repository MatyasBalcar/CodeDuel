package dev.codeduel.battles;

import dev.codeduel.common.ApiException;
import dev.codeduel.common.DatabaseClock;
import dev.codeduel.events.BattleEvents;
import dev.codeduel.problems.ProblemService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BattleService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private final JdbcClient jdbc;
  private final BattleStore battles;
  private final SessionGuard sessions;
  private final ProblemService problems;
  private final DatabaseClock clock;
  private final BattleEvents events;
  private final FinalizeBattleService finalizer;
  private final int readySeconds;
  private final int startDelaySeconds;

  public BattleService(
      JdbcClient jdbc,
      BattleStore battles,
      SessionGuard sessions,
      ProblemService problems,
      DatabaseClock clock,
      BattleEvents events,
      FinalizeBattleService finalizer,
      @Value("${codeduel.battle.ready-seconds}") int readySeconds,
      @Value("${codeduel.battle.start-delay-seconds}") int startDelaySeconds) {
    this.jdbc = jdbc;
    this.battles = battles;
    this.sessions = sessions;
    this.problems = problems;
    this.clock = clock;
    this.events = events;
    this.finalizer = finalizer;
    this.readySeconds = readySeconds;
    this.startDelaySeconds = startDelaySeconds;
  }

  @Transactional
  public PrivateRoom createPrivate(UUID user) {
    return createPrivateInternal(user, null);
  }

  private PrivateRoom createPrivateInternal(UUID user, UUID opponent) {
    sessions.lockUser(user);
    UUID id = UUID.randomUUID();
    byte[] bytes = new byte[24];
    RANDOM.nextBytes(bytes);
    String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    insertBattle(id, problems.select(user, opponent), "PRIVATE", "WAITING", hash(code), 300);
    sessions.reserve(user, id);
    addParticipant(id, user, 1);
    events.append(id, "ROOM_CREATED", Map.of("userId", user));
    return new PrivateRoom(id, code);
  }

  @Transactional
  public UUID joinPrivate(UUID user, String code) {
    if (!code.matches("[A-Za-z0-9_-]{32}")) throw ApiException.notFound();
    sessions.lockUser(user);
    UUID id =
        jdbc.sql("SELECT id FROM battles WHERE invite_hash = :hash")
            .param("hash", hash(code))
            .query(UUID.class)
            .optional()
            .orElseThrow(ApiException::notFound);
    return joinById(user, id);
  }

  private UUID joinById(UUID user, UUID id) {
    var battle = battles.lock(id);
    boolean alreadyMember =
        jdbc.sql(
                "SELECT EXISTS(SELECT 1 FROM battle_participants WHERE battle_id = :id AND user_id = :user)")
            .param("id", id)
            .param("user", user)
            .query(Boolean.class)
            .single();
    if (alreadyMember) return id;
    if (battle.status() != BattleStatus.WAITING || !clock.now().isBefore(battle.readyDeadline())) {
      throw ApiException.conflict("INVITE_EXPIRED", "This room is full or its invite has expired.");
    }
    sessions.reserve(user, id);
    addParticipant(id, user, 2);
    jdbc.sql(
            "UPDATE battles SET status = 'READY_CHECK', ready_deadline = clock_timestamp() + :seconds * interval '1 second' WHERE id = :id")
        .param("seconds", readySeconds)
        .param("id", id)
        .update();
    events.append(id, "MATCH_FOUND", Map.of("userId", user));
    return id;
  }

  public RematchProposal rematchProposal(UUID origin, UUID user) {
    battles.requireMember(origin, user);
    return jdbc.sql(
            """
        SELECT r.battle_id,r.proposed_by FROM rematch_rooms r JOIN battles b ON b.id=r.battle_id
        WHERE r.origin_battle_id=:id AND b.status NOT IN ('FINISHED','CANCELLED')
          AND (b.starts_at IS NOT NULL OR b.ready_deadline > clock_timestamp())
        """)
        .param("id", origin)
        .query(
            (rs, row) ->
                new RematchProposal(
                    rs.getObject("battle_id", UUID.class), rs.getObject("proposed_by", UUID.class)))
        .optional()
        .orElse(null);
  }

  @Transactional
  public PrivateRoom rematch(UUID origin, UUID user) {
    var previous = battles.lock(origin);
    battles.requireMember(origin, user);
    if (!previous.status().terminal())
      throw ApiException.conflict(
          "BATTLE_NOT_FINISHED", "Finish the current battle before requesting a rematch.");
    sessions.lockUser(user);
    UUID opponent =
        jdbc.sql("SELECT user_id FROM battle_participants WHERE battle_id=:id AND user_id<>:user")
            .param("id", origin)
            .param("user", user)
            .query(UUID.class)
            .optional()
            .orElseThrow(
                () -> ApiException.conflict("NO_OPPONENT", "This room never had an opponent."));
    UUID existing =
        jdbc.sql("SELECT battle_id FROM rematch_rooms WHERE origin_battle_id=:id")
            .param("id", origin)
            .query(UUID.class)
            .optional()
            .orElse(null);
    if (existing != null) {
      finalizer.attempt(existing);
      var current = battles.lock(existing);
      if (!current.status().terminal()) {
        return new PrivateRoom(joinById(user, existing), null);
      }
    }
    var room = createPrivateInternal(user, opponent);
    jdbc.sql(
            "INSERT INTO rematch_rooms(origin_battle_id,battle_id,proposed_by) VALUES (:origin,:battle,:user) ON CONFLICT (origin_battle_id) DO UPDATE SET battle_id=EXCLUDED.battle_id,proposed_by=EXCLUDED.proposed_by")
        .param("origin", origin)
        .param("battle", room.battleId())
        .param("user", user)
        .update();
    return room;
  }

  public record RematchProposal(UUID battleId, UUID proposedBy) {}

  /** Matchmaker owns both queue rows and calls this within its transaction. */
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public UUID createRanked(UUID first, UUID second) {
    UUID id = UUID.randomUUID();
    insertBattle(id, problems.select(first, second), "RANKED", "READY_CHECK", null, readySeconds);
    addParticipant(id, first, 1);
    addParticipant(id, second, 2);
    int changed =
        jdbc.sql(
                "UPDATE active_sessions SET battle_id = :id WHERE user_id IN (:first, :second) AND battle_id IS NULL")
            .param("id", id)
            .param("first", first)
            .param("second", second)
            .update();
    if (changed != 2)
      throw new IllegalStateException("Queue participants lost their active-session claims");
    events.append(id, "MATCH_FOUND", Map.of());
    return id;
  }

  @Transactional
  public void ready(UUID user, UUID id) {
    var battle = battles.lock(id);
    battles.requireMember(id, user);
    if (battle.status() == BattleStatus.IN_PROGRESS) return;
    if (battle.status() != BattleStatus.READY_CHECK
        || !clock.now().isBefore(battle.readyDeadline())) {
      throw ApiException.conflict("READY_CLOSED", "This ready check has closed.");
    }
    int changed =
        jdbc.sql(
                "UPDATE battle_participants SET is_ready = true WHERE battle_id = :id AND user_id = :user AND NOT is_ready")
            .param("id", id)
            .param("user", user)
            .update();
    if (changed == 0) return;
    events.append(id, "PLAYER_READY", Map.of("userId", user));
    int ready =
        jdbc.sql("SELECT count(*) FROM battle_participants WHERE battle_id = :id AND is_ready")
            .param("id", id)
            .query(Integer.class)
            .single();
    if (ready == 2) {
      jdbc.sql(
              """
            WITH instant AS (SELECT clock_timestamp() AS now)
            UPDATE battles SET status = 'IN_PROGRESS',
            starts_at = instant.now + :delay * interval '1 second',
            ends_at = instant.now + (:delay + (SELECT duration_seconds FROM problem_versions WHERE id = :version)) * interval '1 second'
          FROM instant WHERE id = :id
          """)
          .param("delay", startDelaySeconds)
          .param("version", battle.problemVersionId())
          .param("id", id)
          .update();
      var started = battles.lock(id);
      events.append(
          id,
          "BATTLE_STARTED",
          Map.of("startsAt", started.startsAt().toString(), "endsAt", started.endsAt().toString()));
    }
  }

  @Transactional
  public void forfeit(UUID user, UUID id) {
    var battle = battles.lock(id);
    battles.requireMember(id, user);
    if (battle.status().terminal()) return;
    if (battle.startsAt() == null) {
      finalizer.cancel(id, "PLAYER_LEFT");
      return;
    }
    if (!clock.now().isBefore(battle.endsAt())) {
      // Expired games must resolve from their submissions rather than a late forfeit.
      finalizer.attempt(id);
      return;
    }
    jdbc.sql(
            "UPDATE battle_participants SET forfeited_at = clock_timestamp() WHERE battle_id = :id AND user_id = :user AND forfeited_at IS NULL")
        .param("id", id)
        .param("user", user)
        .update();
    events.append(id, "PLAYER_FORFEITED", Map.of("userId", user));
    finalizer.attempt(id);
  }

  private void insertBattle(
      UUID id, UUID version, String mode, String status, String inviteHash, int seconds) {
    jdbc.sql(
            "INSERT INTO battles(id, problem_version_id, mode, status, invite_hash, ready_deadline) VALUES (:id, :version, :mode, :status, :hash, clock_timestamp() + :seconds * interval '1 second')")
        .param("id", id)
        .param("version", version)
        .param("mode", mode)
        .param("status", status)
        .param("hash", inviteHash, java.sql.Types.VARCHAR)
        .param("seconds", seconds)
        .update();
  }

  private void addParticipant(UUID battle, UUID user, int slot) {
    jdbc.sql(
            "INSERT INTO battle_participants(battle_id, user_id, slot) VALUES (:battle, :user, :slot)")
        .param("battle", battle)
        .param("user", user)
        .param("slot", slot)
        .update();
  }

  private static String hash(String code) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public record PrivateRoom(UUID battleId, String inviteCode) {}
}
