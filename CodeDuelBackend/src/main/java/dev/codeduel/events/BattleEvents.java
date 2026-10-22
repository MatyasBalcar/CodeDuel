package dev.codeduel.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.codeduel.battles.BattleStore;
import dev.codeduel.common.Sql;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BattleEvents {
  private final JdbcClient jdbc;
  private final ObjectMapper json;
  private final BattleStore battles;

  public BattleEvents(JdbcClient jdbc, ObjectMapper json, BattleStore battles) {
    this.jdbc = jdbc;
    this.json = json;
    this.battles = battles;
  }

  /** Payloads are a deliberately public projection shared by both participants. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void append(UUID battleId, String type, Map<String, ?> payload) {
    long sequence =
        jdbc.sql(
                "UPDATE battles SET event_sequence = event_sequence + 1 WHERE id = :id RETURNING event_sequence")
            .param("id", battleId)
            .query(Long.class)
            .single();
    try {
      jdbc.sql(
              "INSERT INTO battle_events(battle_id, sequence, event_type, payload_json) VALUES (:id, :sequence, :type, CAST(:payload AS jsonb))")
          .param("id", battleId)
          .param("sequence", sequence)
          .param("type", type)
          .param("payload", json.writeValueAsString(payload))
          .update();
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Cannot serialize battle event", ex);
    }
  }

  public List<Event> since(UUID battleId, UUID userId, long after) {
    battles.requireMember(battleId, userId);
    return jdbc.sql(
            "SELECT * FROM battle_events WHERE battle_id = :id AND sequence > :after ORDER BY sequence LIMIT 200")
        .param("id", battleId)
        .param("after", Math.max(0, after))
        .query(
            (rs, row) -> {
              try {
                return new Event(
                    battleId,
                    rs.getLong("sequence"),
                    rs.getString("event_type"),
                    Sql.instant(rs, "created_at"),
                    json.readTree(rs.getString("payload_json")));
              } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Invalid stored event", ex);
              }
            })
        .list();
  }

  public record Event(
      UUID battleId, long sequence, String type, Instant occurredAt, JsonNode payload) {}
}
