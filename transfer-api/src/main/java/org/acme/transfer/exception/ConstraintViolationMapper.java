package org.acme.transfer.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.Instant;
import org.acme.transfer.dto.response.ErrorResponse;

@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {
  @Context UriInfo uriInfo;

  @Override
  public Response toResponse(ConstraintViolationException e) {
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(
            new ErrorResponse(
                Instant.now(),
                400,
                "VALIDATION_ERROR",
                "Request validation failed",
                uriInfo.getPath()))
        .build();
  }
}
