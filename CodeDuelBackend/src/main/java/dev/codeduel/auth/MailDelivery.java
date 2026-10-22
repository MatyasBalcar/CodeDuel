package dev.codeduel.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnWebApplication
@ConditionalOnProperty(
    name = "codeduel.scheduling-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class MailDelivery {
  private final MailOutbox outbox;
  private final JavaMailSender sender;
  private final String from;

  public MailDelivery(
      MailOutbox outbox,
      JavaMailSender sender,
      @Value("${codeduel.mail.from:noreply@codeduel.local}") String from) {
    this.outbox = outbox;
    this.sender = sender;
    this.from = from;
  }

  @Scheduled(fixedDelay = 1000, initialDelay = 5000)
  public void tick() {
    outbox
        .claim()
        .ifPresent(
            message -> {
              boolean sent = false;
              try {
                var mail = new SimpleMailMessage();
                mail.setFrom(from);
                mail.setTo(message.recipient());
                mail.setSubject(message.subject());
                mail.setText(message.body());
                sender.send(mail);
                sent = true;
              } catch (org.springframework.mail.MailException ignored) {
                /* Never log addresses, links, credentials or transport bodies. */
              }
              outbox.finish(message, sent);
            });
  }
}
