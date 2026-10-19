package org.acme.transfer.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class FrankfurterExchangeRateProvider implements ExchangeRateProvider {
  @Inject @RestClient ExchangeRateClient client;

  @Override
  public BigDecimal getRate(String sourceCurrency, String targetCurrency) {
    ExchangeRateClient.RateResponse response =
        client.rate(sourceCurrency.toLowerCase(), targetCurrency.toLowerCase());
    return response == null ? null : response.rate();
  }
}
