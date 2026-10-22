package dev.codeduel.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnWebApplication
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestBodyLimit extends OncePerRequestFilter {
  private static final int LIMIT = 96 * 1024;
  private final java.util.concurrent.Semaphore adminBodies = new java.util.concurrent.Semaphore(2);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!(request.getMethod().equals("POST")
        || request.getMethod().equals("PUT")
        || request.getMethod().equals("PATCH"))) {
      chain.doFilter(request, response);
      return;
    }
    boolean admin = request.getRequestURI().startsWith("/api/v1/admin/problems");
    if (admin && !adminBodies.tryAcquire()) {
      response.setStatus(429);
      response.setContentType("application/problem+json");
      response
          .getWriter()
          .write(
              "{\"status\":429,\"code\":\"ADMIN_BUSY\",\"detail\":\"Wait for the current problem upload to finish.\"}");
      return;
    }
    try {
      bounded(request, response, chain, admin ? 4 * 1024 * 1024 : LIMIT);
    } finally {
      if (admin) adminBodies.release();
    }
  }

  private void bounded(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain, int limit)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > limit) {
      reject(response);
      return;
    }
    byte[] bytes = request.getInputStream().readNBytes(limit + 1);
    if (bytes.length > limit) {
      reject(response);
      return;
    }
    chain.doFilter(
        new HttpServletRequestWrapper(request) {
          @Override
          public ServletInputStream getInputStream() {
            var input = new ByteArrayInputStream(bytes);
            return new ServletInputStream() {
              @Override
              public int read() {
                return input.read();
              }

              @Override
              public int read(byte[] buffer, int offset, int count) {
                return input.read(buffer, offset, count);
              }

              @Override
              public boolean isFinished() {
                return input.available() == 0;
              }

              @Override
              public boolean isReady() {
                return true;
              }

              @Override
              public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException("Synchronous JSON bodies only");
              }
            };
          }

          @Override
          public BufferedReader getReader() {
            return new BufferedReader(
                new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
          }
        },
        response);
  }

  private static void reject(HttpServletResponse response) throws IOException {
    response.setStatus(413);
    response.setContentType("application/problem+json");
    response
        .getWriter()
        .write(
            "{\"status\":413,\"code\":\"PAYLOAD_TOO_LARGE\",\"detail\":\"Request body exceeds the limit for this endpoint.\"}");
  }
}
