package org.acme.transfer.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.UUID;
import org.acme.transfer.service.TransferService;

@RolesAllowed("ADMIN")
@Path("/api/v1/admin/transfers")
public class AdminTransferResource {
  @Inject TransferService transfers;

  @GET
  public Response list(
      @QueryParam("status") String status,
      @QueryParam("customerId") UUID customerId,
      @QueryParam("from") Instant from,
      @QueryParam("to") Instant to) {
    return Response.ok(transfers.adminList(status, customerId, from, to)).build();
  }
}
