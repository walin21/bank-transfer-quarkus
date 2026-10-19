package org.acme.transfer.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.acme.transfer.service.AccountService;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Authenticated
@Path("/api/v1/accounts")
public class AccountResource {
  @Inject JsonWebToken token;
  @Inject AccountService accounts;

  @GET
  public Response list() {
    return Response.ok(accounts.findByCustomer(java.util.UUID.fromString(token.getSubject())))
        .build();
  }
}
