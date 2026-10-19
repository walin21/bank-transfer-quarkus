package org.acme.transfer.client;

import java.math.BigDecimal;

/** Application-facing exchange-rate port. Provider-specific DTOs stay in the adapter. */
public interface ExchangeRateProvider {
  BigDecimal getRate(String sourceCurrency, String targetCurrency);
}
