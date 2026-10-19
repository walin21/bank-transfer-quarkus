package org.acme.transfer.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
    @NotNull UUID sourceAccountId,
    @NotNull UUID beneficiaryId,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {}
