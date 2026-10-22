package dev.codeduel.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnWebApplication
@ConditionalOnProperty(
    name = "codeduel.scheduling-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AccountMaintenance {
  private final AccountDataService data;

  public AccountMaintenance(AccountDataService data) {
    this.data = data;
  }

  @Scheduled(cron = "0 0 * * * *", zone = "UTC")
  public void clean() {
    data.purgeExpiredSources();
  }
}
