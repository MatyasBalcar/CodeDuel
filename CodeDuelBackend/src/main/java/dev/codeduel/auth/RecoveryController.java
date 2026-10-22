package dev.codeduel.auth;

import dev.codeduel.common.ClientAddress;
import dev.codeduel.common.RateLimiter;
import dev.codeduel.common.SecretTokens;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class RecoveryController {
  private final AccountRecovery recovery;
  private final AccountDataService data;
  private final RateLimiter limits;
  private final ClientAddress addresses;

  public RecoveryController(
      AccountRecovery recovery,
      AccountDataService data,
      RateLimiter limits,
      ClientAddress addresses) {
    this.recovery = recovery;
    this.data = data;
    this.limits = limits;
    this.addresses = addresses;
  }

  @PostMapping("/forgot-password")
  Map<String, String> forgot(@Valid @RequestBody EmailAddress body, HttpServletRequest request) {
    limits.check("reset-ip:" + addresses.of(request), 10, Duration.ofHours(1));
    limits.check(
        "reset-email:" + SecretTokens.hash(body.email().toLowerCase(java.util.Locale.ROOT)),
        3,
        Duration.ofHours(1));
    recovery.requestReset(body.email());
    return Map.of(
        "message", "If this address belongs to an account, a reset link will arrive shortly.");
  }

  @PostMapping("/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void reset(@Valid @RequestBody Reset body, HttpServletRequest request) {
    limits.check("reset-consume:" + addresses.of(request), 20, Duration.ofHours(1));
    recovery.reset(body.token(), body.password());
    clearSession(request);
  }

  @PostMapping("/verify-email")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void verify(@Valid @RequestBody Token body, HttpServletRequest request) {
    limits.check("verify-consume:" + addresses.of(request), 30, Duration.ofHours(1));
    recovery.verify(body.token());
  }

  @PostMapping("/verification-email")
  @ResponseStatus(HttpStatus.ACCEPTED)
  void resend(@AuthenticationPrincipal AccountPrincipal user) {
    limits.check("verify-resend:" + user.id(), 3, Duration.ofHours(1));
    recovery.requestVerification(user.id());
  }

  @GetMapping("/account")
  AccountDataService.Account account(@AuthenticationPrincipal AccountPrincipal user) {
    return data.account(user.id());
  }

  @PostMapping("/delete-account")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(
      @AuthenticationPrincipal AccountPrincipal user,
      @Valid @RequestBody Password body,
      HttpServletRequest request) {
    limits.check("delete-account:" + user.id(), 5, Duration.ofHours(1));
    data.delete(user.id(), body.password());
    clearSession(request);
  }

  private void clearSession(HttpServletRequest request) {
    var session = request.getSession(false);
    if (session != null) session.invalidate();
    SecurityContextHolder.clearContext();
  }

  record EmailAddress(@Email @NotBlank @Size(max = 254) String email) {}

  record Token(@Pattern(regexp = "[A-Za-z0-9_-]{43}") @NotBlank String token) {}

  record Reset(
      @Pattern(regexp = "[A-Za-z0-9_-]{43}") @NotBlank String token,
      @NotBlank @Size(min = 12, max = 72) String password) {}

  record Password(@NotBlank @Size(min = 12, max = 72) String password) {}
}
