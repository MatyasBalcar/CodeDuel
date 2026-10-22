package dev.codeduel.auth;

import dev.codeduel.common.ApiException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;

public final class PasswordPolicy {
  private PasswordPolicy() {}

  public static void validate(String password) {
    if (password == null
        || password.length() < 12
        || password.length() > 72
        || password.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "INVALID_PASSWORD",
          "Use a password of 12–72 characters and at most 72 UTF-8 bytes.");
    }
  }
}
