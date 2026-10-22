package dev.codeduel.common;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditLog {
  private final JdbcClient jdbc;

  public AuditLog(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void append(UUID actor, String action, String entityType, UUID entity) {
    jdbc.sql(
            "INSERT INTO audit_log(id, actor_id, action, entity_type, entity_id) VALUES (:id,:actor,:action,:type,:entity)")
        .param("id", UUID.randomUUID())
        .param("actor", actor)
        .param("action", action)
        .param("type", entityType)
        .param("entity", entity)
        .update();
  }
}
