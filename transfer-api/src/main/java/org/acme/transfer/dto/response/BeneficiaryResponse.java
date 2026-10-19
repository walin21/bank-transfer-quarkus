package org.acme.transfer.dto.response;

import java.time.Instant;
import java.util.UUID;

public record BeneficiaryResponse(
    UUID id,
    UUID customerId,
    String alias,
    String bankName,
    String accountNumber,
    String currency,
    String status,
    Instant createdAt) {}
