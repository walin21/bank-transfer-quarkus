package org.acme.transfer.service;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.acme.transfer.dto.request.LoginRequest;
import org.acme.transfer.dto.response.LoginResponse;
import org.acme.transfer.entity.Customer;
import org.acme.transfer.entity.CustomerStatus;
import org.acme.transfer.exception.ApiException;
import org.acme.transfer.repository.CustomerRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AuthenticationService {
  @Inject CustomerRepository customers;

  @ConfigProperty(name = "app.security.demo-password")
  String demoPassword;

  @ConfigProperty(name = "app.security.admin-email")
  String adminEmail;

  @ConfigProperty(name = "app.security.jwt.issuer")
  String issuer;

  @ConfigProperty(name = "app.security.jwt.expiration")
  Duration expiration;

  @ConfigProperty(name = "app.security.jwt.secret")
  String jwtSecret;

  public LoginResponse login(LoginRequest request) {
    Customer customer = customers.find("email = ?1", request.email()).firstResult();
    if (customer == null
        || customer.status != CustomerStatus.ACTIVE
        || !demoPassword.equals(request.password())) {
      throw new ApiException(
          Response.Status.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials");
    }
    Instant now = Instant.now();
    long seconds = expiration.toSeconds();
    Set<String> groups =
        adminEmail.equalsIgnoreCase(customer.email)
            ? Set.of("CUSTOMER", "ADMIN")
            : Set.of("CUSTOMER");
    String token =
        Jwt.subject(customer.id.toString())
            .issuer(issuer)
            .groups(groups)
            .issuedAt(now)
            .expiresAt(now.plus(expiration))
            .jws()
            .algorithm(SignatureAlgorithm.HS256)
            .signWithSecret(jwtSecret);
    return new LoginResponse(token, "Bearer", seconds);
  }
}
