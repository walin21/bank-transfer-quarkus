package org.acme.transfer.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.config.spi.ConfigSource;

public final class JwtVerificationConfigSource implements ConfigSource {
  private final Map<String, String> properties;

  public JwtVerificationConfigSource() {
    String secret = System.getenv("JWT_SECRET");
    if (secret == null || secret.isBlank()) {
      // Tests provide their own JWK through %test configuration. In runtime,
      // application.properties requires JWT_SECRET to be explicitly defined.
      properties = Map.of();
      return;
    }
    String encodedSecret =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(secret.getBytes(StandardCharsets.UTF_8));
    String jwk = "{\"keys\":[{\"kty\":\"oct\",\"k\":\"" + encodedSecret + "\"}]}";
    properties =
        Map.of("mp.jwt.verify.publickey", jwk, "mp.jwt.verify.publickey.algorithm", "HS256");
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public String getValue(String propertyName) {
    return properties.get(propertyName);
  }

  @Override
  public Set<String> getPropertyNames() {
    return properties.keySet();
  }

  @Override
  public String getName() {
    return "bank-transfer-jwt-verification";
  }

  @Override
  public int getOrdinal() {
    return 500;
  }
}
