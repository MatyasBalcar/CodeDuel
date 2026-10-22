package dev.codeduel.judge;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.codeduel.problems.ProblemCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
@EnableConfigurationProperties(JudgeSettings.class)
public class JudgeConfiguration {
  @Bean
  JudgeGateway judgeGateway(
      JudgeSettings settings,
      ProblemCatalog catalog,
      ObjectMapper json,
      Environment environment,
      @Value("${codeduel.worker-enabled:false}") boolean workerEnabled) {
    if (workerEnabled && settings.mode() == JudgeSettings.Mode.DISABLED) {
      throw new IllegalStateException("A worker requires an enabled judge adapter");
    }
    if (!workerEnabled
        && (settings.mode() == JudgeSettings.Mode.GO_JUDGE
            || settings.mode() == JudgeSettings.Mode.JUDGE0)) {
      return (request, expected, token) -> {
        throw new JudgeUnavailableException();
      };
    }
    return switch (settings.mode()) {
      case DISABLED ->
          (request, expected, token) -> {
            throw new JudgeUnavailableException();
          };
      case FIXTURE -> {
        if (!environment.acceptsProfiles(Profiles.of("demo")))
          throw new IllegalStateException("Fixture judge requires the explicit demo profile");
        yield new FixtureJudgeGateway(catalog);
      }
      case JUDGE0 -> {
        var adapter = new Judge0Gateway(settings, json);
        adapter.verifyRuntime();
        yield adapter;
      }
      case GO_JUDGE -> {
        var adapter = new GoJudgeGateway(settings, json);
        if (workerEnabled) adapter.verifyRuntime();
        yield adapter;
      }
    };
  }
}
