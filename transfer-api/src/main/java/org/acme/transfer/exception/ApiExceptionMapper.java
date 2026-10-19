package org.acme.transfer.exception;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import org.acme.transfer.dto.response.ErrorResponse;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {
  @Context UriInfo uriInfo;

  @Override
  public Response toResponse(ApiException e) {
    return Response.status(e.status())
        .entity(
            new ErrorResponse(
                Instant.now(),
                e.status().getStatusCode(),
                e.code(),
                e.getMessage(),
                uriInfo.getPath()))
        .build();
  }
}
