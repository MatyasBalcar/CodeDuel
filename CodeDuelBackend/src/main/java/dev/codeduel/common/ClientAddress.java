package dev.codeduel.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientAddress {
  private final boolean trustEdge;

  public ClientAddress(@Value("${codeduel.trust-edge-address:false}") boolean trustEdge) {
    this.trustEdge = trustEdge;
  }

  public String of(HttpServletRequest request) {
    String address = request.getHeader("X-CodeDuel-Client-IP");
    if (trustEdge && address != null && address.length() <= 45 && address.matches("[0-9a-fA-F:.]+"))
      return address;
    return request.getRemoteAddr();
  }
}
