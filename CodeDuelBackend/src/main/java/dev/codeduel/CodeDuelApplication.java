package dev.codeduel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CodeDuelApplication {
  public static void main(String[] args) {
    SpringApplication.run(CodeDuelApplication.class, args);
  }
}
