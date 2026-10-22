package dev.codeduel.common;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("migrate")
public class MigrationProcess {
  @EventListener
  public void finished(ApplicationReadyEvent event) {
    event.getApplicationContext().close();
  }
}
