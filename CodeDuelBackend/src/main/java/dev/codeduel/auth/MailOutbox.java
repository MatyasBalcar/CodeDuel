package dev.codeduel.auth;

import dev.codeduel.common.Sql;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MailOutbox {
  private final JdbcClient jdbc;

  public MailOutbox(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public Optional<Message> claim() {
    UUID lease = UUID.randomUUID();
    return jdbc.sql(
            """
        WITH candidate AS (SELECT id FROM mail_outbox WHERE (status='PENDING' AND next_retry_at<=clock_timestamp())
          OR (status='SENDING' AND lease_until<=clock_timestamp()) ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1)
        UPDATE mail_outbox m SET status='SENDING',lease_token=:lease,lease_until=clock_timestamp()+interval '30 seconds',attempts=attempts+1
        FROM candidate WHERE m.id=candidate.id RETURNING m.id,m.recipient,m.subject,m.body
        """)
        .param("lease", lease)
        .query(
            (rs, row) ->
                new Message(
                    Sql.uuid(rs, "id"),
                    lease,
                    rs.getString("recipient"),
                    rs.getString("subject"),
                    rs.getString("body")))
        .optional();
  }

  @Transactional
  public void finish(Message message, boolean sent) {
    jdbc.sql(
            """
        UPDATE mail_outbox SET status=CASE WHEN :sent THEN 'SENT' WHEN attempts>=3 THEN 'DEAD' ELSE 'PENDING' END,
          recipient=CASE WHEN :sent THEN '' ELSE recipient END,body=CASE WHEN :sent THEN '' ELSE body END,
          lease_token=NULL,lease_until=NULL,next_retry_at=clock_timestamp()+interval '30 seconds'
        WHERE id=:id AND status='SENDING' AND lease_token=:lease AND lease_until>clock_timestamp()
        """)
        .param("sent", sent)
        .param("id", message.id())
        .param("lease", message.lease())
        .update();
  }

  public record Message(UUID id, UUID lease, String recipient, String subject, String body) {}
}
