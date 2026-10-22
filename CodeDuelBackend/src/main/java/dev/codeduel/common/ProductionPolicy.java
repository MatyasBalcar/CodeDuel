package dev.codeduel.common;

import dev.codeduel.judge.JudgeSettings;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class ProductionPolicy implements ApplicationRunner {
  private final Environment environment;
  private final JudgeSettings judge;

  public ProductionPolicy(Environment environment, JudgeSettings judge) {
    this.environment = environment;
    this.judge = judge;
  }

  @Override
  public void run(ApplicationArguments args) {
    String publicUrl = environment.getProperty("codeduel.public-url", "");
    boolean webApi = !environment.acceptsProfiles(Profiles.of("worker"));
    boolean localMail = environment.getProperty("codeduel.mail.local-only", Boolean.class, false);
    boolean smtpTls =
        environment.getProperty(
            "spring.mail.properties.mail.smtp.starttls.required", Boolean.class, false);
    if (!publicUrl.startsWith("https://")
        || (webApi
            && localMail
            && (!publicUrl.startsWith("https://localhost:")
                || !"mailpit".equals(environment.getProperty("spring.mail.host"))))
        || (webApi && !localMail && !smtpTls)) {
      throw new IllegalStateException(
          "Production requires a canonical HTTPS URL and encrypted SMTP; local Mailpit is restricted to the localhost rehearsal");
    }
    if (environment.acceptsProfiles(Profiles.of("demo"))
        || judge.mode() == JudgeSettings.Mode.FIXTURE
        || judge.mode() == JudgeSettings.Mode.DISABLED
        || environment.getProperty("spring.datasource.password", "").length() < 24
        || environment.getProperty("spring.flyway.enabled", Boolean.class, true)
        || environment.getProperty("codeduel.seed-enabled", Boolean.class, true)
        || !environment.getProperty("server.servlet.session.cookie.secure", Boolean.class, false)) {
      throw new IllegalStateException(
          "Production requires real judging, secure sessions, strong database credentials and separate migrations");
    }
  }
}
