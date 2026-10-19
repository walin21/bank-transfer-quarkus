package org.acme.transfer.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.acme.transfer.dto.request.LoginRequest;
import org.acme.transfer.service.AuthenticationService;

@Path("/api/v1/auth")
public class AuthResource {
  @Inject AuthenticationService authentication;

  @POST
  @Path("/login")
  public Response login(@Valid LoginRequest request) {
    return Response.ok(authentication.login(request)).build();
  }
}
