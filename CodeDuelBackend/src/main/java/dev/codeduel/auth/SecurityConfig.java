package dev.codeduel.auth;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService userDetailsService(JdbcClient jdbc) {
    return username ->
        jdbc.sql(
                "SELECT id, username, password_hash, role FROM users WHERE lower(username) = lower(:username) AND deleted_at IS NULL")
            .param("username", username)
            .query(
                (rs, row) ->
                    new AccountPrincipal(
                        rs.getObject("id", java.util.UUID.class),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role")))
            .optional()
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
  }

  @Bean
  AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder passwords) {
    var provider = new DaoAuthenticationProvider(users);
    provider.setPasswordEncoder(passwords);
    return new ProviderManager(provider);
  }

  @Bean
  CsrfTokenRepository csrfTokenRepository() {
    return new HttpSessionCsrfTokenRepository();
  }

  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrf) {
    return new CompositeSessionAuthenticationStrategy(
        List.of(new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrf)));
  }

  @Bean
  @ConditionalOnWebApplication
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      CsrfTokenRepository csrf,
      SecurityContextRepository contexts,
      @Value("${codeduel.metrics-token:}") String metricsToken)
      throws Exception {
    return http.addFilterBefore(
            new MetricsTokenFilter(metricsToken),
            org.springframework.security.web.access.intercept.AuthorizationFilter.class)
        .csrf(config -> config.csrfTokenRepository(csrf))
        .securityContext(config -> config.securityContextRepository(contexts))
        .authorizeHttpRequests(
            auth ->
                auth.dispatcherTypeMatchers(
                        jakarta.servlet.DispatcherType.ASYNC, jakarta.servlet.DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(
                        "/api/v1/auth/csrf",
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/me",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password",
                        "/api/v1/auth/verify-email",
                        "/api/v1/system",
                        "/api/v1/problems",
                        "/api/v1/problems/*",
                        "/api/v1/leaderboard",
                        "/api/v1/users/*",
                        "/api/v1/users/*/rating-history",
                        "/actuator/health")
                    .permitAll()
                    .requestMatchers("/actuator/prometheus")
                    .hasAnyRole("ADMIN", "METRICS")
                    .requestMatchers("/api/v1/admin/**", "/actuator/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .requestCache(config -> config.disable())
        .exceptionHandling(
            config ->
                config
                    .authenticationEntryPoint(
                        (request, response, ex) -> {
                          response.setStatus(401);
                          response.setContentType("application/problem+json");
                          response
                              .getWriter()
                              .write(
                                  "{\"status\":401,\"code\":\"UNAUTHENTICATED\",\"detail\":\"Please sign in to continue.\"}");
                        })
                    .accessDeniedHandler(
                        (request, response, ex) -> {
                          response.setStatus(403);
                          response.setContentType("application/problem+json");
                          response
                              .getWriter()
                              .write(
                                  "{\"status\":403,\"code\":\"FORBIDDEN\",\"detail\":\"This request is not permitted. Refresh and try again.\"}");
                        }))
        .logout(
            config ->
                config
                    .logoutUrl("/api/v1/auth/logout")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
        .headers(
            config ->
                config.contentSecurityPolicy(
                    csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
        .build();
  }
}
