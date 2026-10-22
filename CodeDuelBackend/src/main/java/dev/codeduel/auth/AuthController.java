package dev.codeduel.auth;

import dev.codeduel.common.ApiException;
import dev.codeduel.common.ClientAddress;
import dev.codeduel.common.RateLimiter;
import dev.codeduel.users.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthenticationManager authentication;
  private final SecurityContextRepository contexts;
  private final SessionAuthenticationStrategy sessions;
  private final AccountService accounts;
  private final UserService users;
  private final RateLimiter limits;
  private final ClientAddress addresses;

  public AuthController(
      AuthenticationManager authentication,
      SecurityContextRepository contexts,
      SessionAuthenticationStrategy sessions,
      AccountService accounts,
      UserService users,
      RateLimiter limits,
      ClientAddress addresses) {
    this.authentication = authentication;
    this.contexts = contexts;
    this.sessions = sessions;
    this.accounts = accounts;
    this.users = users;
    this.limits = limits;
    this.addresses = addresses;
  }

  @GetMapping("/csrf")
  Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  void register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request) {
    limits.check("register:" + addresses.of(request), 8, Duration.ofHours(1));
    accounts.register(body.username(), body.email(), body.password());
  }

  @PostMapping("/login")
  UserService.Profile login(
      @Valid @RequestBody LoginRequest body,
      HttpServletRequest request,
      HttpServletResponse response) {
    limits.check("login:" + addresses.of(request), 40, Duration.ofMinutes(15));
    var result =
        authentication.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
    sessions.onAuthentication(result, request, response);
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(result);
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, request, response);
    return users.profile(((AccountPrincipal) result.getPrincipal()).id());
  }

  @GetMapping("/me")
  UserService.Profile me(@AuthenticationPrincipal AccountPrincipal principal) {
    if (principal == null)
      throw new ApiException(
          HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Please sign in to continue.");
    return users.profile(principal.id());
  }

  record RegisterRequest(
      @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_]{2,23}") @NotBlank String username,
      @Email @NotBlank @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 72) String password) {}

  record LoginRequest(
      @NotBlank @Size(max = 24) String username, @NotBlank @Size(max = 72) String password) {}
}
