package org.acme.transfer.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import java.math.BigDecimal;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/v2")
@RegisterRestClient(configKey = "exchange-rate-api")
@Produces("application/json")
public interface ExchangeRateClient {
  @GET
  @Path("/rate/{base}/{quote}")
  RateResponse rate(@PathParam("base") String base, @PathParam("quote") String quote);

  record RateResponse(String date, String base, String quote, BigDecimal rate) {}
}
