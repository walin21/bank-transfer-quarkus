package org.acme.transfer.resource;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.acme.transfer.dto.request.BeneficiaryRequest;
import org.acme.transfer.service.BeneficiaryService;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Authenticated
@Path("/api/v1/beneficiaries")
public class BeneficiaryResource {
  @Inject JsonWebToken token;
  @Inject BeneficiaryService beneficiaries;

  private UUID customer() {
    return UUID.fromString(token.getSubject());
  }

  @GET
  public Response list() {
    return Response.ok(beneficiaries.list(customer())).build();
  }

  @POST
  public Response create(@Valid BeneficiaryRequest request) {
    return Response.status(Response.Status.CREATED)
        .entity(beneficiaries.create(customer(), request))
        .build();
  }

  @GET
  @Path("/{id}")
  public Response get(@PathParam("id") UUID id) {
    return Response.ok(beneficiaries.get(customer(), id)).build();
  }

  @PUT
  @Path("/{id}")
  public Response update(@PathParam("id") UUID id, @Valid BeneficiaryRequest request) {
    return Response.ok(beneficiaries.update(customer(), id, request)).build();
  }

  @DELETE
  @Path("/{id}")
  public Response delete(@PathParam("id") UUID id) {
    beneficiaries.deactivate(customer(), id);
    return Response.noContent().build();
  }
}
