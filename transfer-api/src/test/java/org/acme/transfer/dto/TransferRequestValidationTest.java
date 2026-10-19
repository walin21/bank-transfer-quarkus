package org.acme.transfer.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.UUID;
import org.acme.transfer.dto.request.BeneficiaryRequest;
import org.acme.transfer.dto.request.TransferRequest;
import org.junit.jupiter.api.Test;

class TransferRequestValidationTest {
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void rejectsMissingIdsAndNonPositiveAmount() {
    var violations = validator.validate(new TransferRequest(null, null, BigDecimal.ZERO));

    assertEquals(3, violations.size());
    assertTrue(
        violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("sourceAccountId")));
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("beneficiaryId")));
    assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
  }

  @Test
  void acceptsValidTransferAndRejectsUnsupportedCurrency() {
    var valid = new TransferRequest(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10.00"));
    var invalidBeneficiary = new BeneficiaryRequest("alias", "bank", "123", "EUR");

    assertTrue(validator.validate(valid).isEmpty());
    assertTrue(
        validator.validate(invalidBeneficiary).stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("currency")));
  }
}
