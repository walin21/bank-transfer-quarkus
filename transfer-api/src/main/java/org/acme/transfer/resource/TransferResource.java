package org.acme.transfer.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.acme.transfer.dto.request.TransferRequest;
import org.acme.transfer.service.TransferService;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Authenticated
@Path("/api/v1/transfers")
public class TransferResource {
  @Inject JsonWebToken token;
  @Inject TransferService transfers;

  private UUID customer() {
    return UUID.fromString(token.getSubject());
  }

  @POST
  public Response create(@Valid TransferRequest request) {
    return Response.status(Response.Status.ACCEPTED)
        .entity(transfers.create(customer(), request))
        .build();
  }

  @GET
  public Response list() {
    return Response.ok(transfers.list(customer())).build();
  }

  @GET
  @Path("/{id}")
  public Response get(@PathParam("id") UUID id) {
    return Response.ok(transfers.get(customer(), id)).build();
  }

  @GET
  @Path("/{id}/history")
  public Response history(@PathParam("id") UUID id) {
    return Response.ok(transfers.history(customer(), id)).build();
  }
}
