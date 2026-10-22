package dev.codeduel.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class MetricsTokenFilter extends OncePerRequestFilter {
  private final String token;

  MetricsTokenFilter(String token) {
    this.token = token;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if ("/actuator/prometheus".equals(request.getRequestURI()) && token.length() >= 32) {
      String supplied = request.getHeader("Authorization");
      if (supplied != null
          && MessageDigest.isEqual(
              ("Bearer " + token).getBytes(StandardCharsets.UTF_8),
              supplied.getBytes(StandardCharsets.UTF_8))) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                "metrics", null, List.of(new SimpleGrantedAuthority("ROLE_METRICS"))));
        SecurityContextHolder.setContext(context);
      }
    }
    chain.doFilter(request, response);
  }
}
