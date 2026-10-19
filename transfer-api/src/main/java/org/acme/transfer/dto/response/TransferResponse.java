package org.acme.transfer.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
    UUID id,
    UUID customerId,
    UUID sourceAccountId,
    UUID beneficiaryId,
    BigDecimal amount,
    String sourceCurrency,
    String targetCurrency,
    BigDecimal exchangeRate,
    BigDecimal convertedAmount,
    String status,
    String failureReason,
    Instant createdAt,
    Instant processedAt) {}
