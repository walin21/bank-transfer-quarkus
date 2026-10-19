package org.acme.transfer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BeneficiaryRequest(
    @NotBlank @Size(max = 80) String alias,
    @NotBlank @Size(max = 120) String bankName,
    @NotBlank @Size(max = 32) String accountNumber,
    @NotBlank @Pattern(regexp = "PEN|USD") String currency) {}
