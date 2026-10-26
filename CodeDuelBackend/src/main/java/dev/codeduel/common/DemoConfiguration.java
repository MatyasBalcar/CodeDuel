package dev.codeduel.common;

import dev.codeduel.auth.AccountPrincipal;
import dev.codeduel.auth.AccountService;
import dev.codeduel.battles.BattleQueries;
import dev.codeduel.problems.ProblemCatalog;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Configuration
@Profile("demo")
public class DemoConfiguration {
  public static final String PASSWORD = "CodeDuel-demo-2026!";

  @Bean
  ApplicationRunner demoUsers(AccountService accounts, JdbcClient jdbc) {
    return args -> {
      for (String name : new String[] {"nova", "atlas"}) {
        boolean exists =
            jdbc.sql("SELECT EXISTS(SELECT 1 FROM users WHERE lower(username) = :name)")
                .param("name", name)
                .query(Boolean.class)
                .single();
        if (!exists) accounts.register(name, name + "@demo.codeduel.invalid", PASSWORD);
        jdbc.sql("UPDATE users SET email_verified=true WHERE lower(username)=:name")
            .param("name", name)
            .update();
      }
    };
  }

  @RestController
  @Profile("demo")
  static class DemoController {
    private final BattleQueries battles;
    private final ProblemCatalog catalog;
    private final dev.codeduel.judge.JudgeSettings judge;

    DemoController(
        BattleQueries battles, ProblemCatalog catalog, dev.codeduel.judge.JudgeSettings judge) {
      this.battles = battles;
      this.catalog = catalog;
      this.judge = judge;
    }

    @GetMapping("/api/v1/demo/practice/{slug}/fixtures")
    Map<String, String> practice(@PathVariable String slug) {
      if (judge.mode() != dev.codeduel.judge.JudgeSettings.Mode.FIXTURE)
        throw ApiException.notFound();
      var problem = catalog.find(slug).orElseThrow(ApiException::notFound);
      return Map.of(
          "acceptedSource",
          problem.referenceSource(),
          "wrongSource",
          ProblemCatalog.wrongSource(problem));
    }

    @GetMapping("/api/v1/demo/battles/{id}/fixtures")
    Map<String, String> fixtures(
        @AuthenticationPrincipal AccountPrincipal user, @PathVariable UUID id) {
      if (judge.mode() != dev.codeduel.judge.JudgeSettings.Mode.FIXTURE)
        throw ApiException.notFound();
      var snapshot = battles.snapshot(id, user.id());
      if (snapshot.problem() == null)
        throw ApiException.conflict(
            "NOT_STARTED", "Sample programs are available after the shared start.");
      var problem =
          catalog.find(snapshot.problem().slug(), snapshot.problem().version()).orElseThrow();
      return Map.of(
          "acceptedSource",
          problem.referenceSource(),
          "wrongSource",
          ProblemCatalog.wrongSource(problem));
    }
  }
}
