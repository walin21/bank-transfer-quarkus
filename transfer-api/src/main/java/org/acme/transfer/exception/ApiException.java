package org.acme.transfer.exception;

import jakarta.ws.rs.core.Response;

public class ApiException extends RuntimeException {
  private final Response.Status status;
  private final String code;

  public ApiException(Response.Status status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public Response.Status status() {
    return status;
  }

  public String code() {
    return code;
  }
}
