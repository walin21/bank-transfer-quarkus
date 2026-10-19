package org.acme.transfer.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.acme.transfer.dto.request.BeneficiaryRequest;
import org.acme.transfer.dto.response.BeneficiaryResponse;
import org.acme.transfer.entity.Beneficiary;
import org.acme.transfer.entity.BeneficiaryStatus;
import org.acme.transfer.exception.ApiException;
import org.acme.transfer.repository.BeneficiaryRepository;

@ApplicationScoped
public class BeneficiaryService {
  @Inject BeneficiaryRepository beneficiaries;

  public List<BeneficiaryResponse> list(UUID customerId) {
    return beneficiaries.find("customerId = ?1 order by createdAt", customerId).stream()
        .map(this::toResponse)
        .toList();
  }

  public BeneficiaryResponse get(UUID customerId, UUID id) {
    return toResponse(find(customerId, id));
  }

  @Transactional
  public BeneficiaryResponse create(UUID customerId, BeneficiaryRequest request) {
    if (beneficiaries
            .find("customerId = ?1 and alias = ?2", customerId, request.alias())
            .firstResult()
        != null)
      throw new ApiException(
          Response.Status.CONFLICT,
          "BENEFICIARY_EXISTS",
          "A beneficiary with this alias already exists");
    Beneficiary b = new Beneficiary();
    b.id = UUID.randomUUID();
    b.customerId = customerId;
    b.alias = request.alias();
    b.bankName = request.bankName();
    b.accountNumber = request.accountNumber();
    b.currency = request.currency();
    b.status = BeneficiaryStatus.ACTIVE;
    b.createdAt = Instant.now();
    beneficiaries.persist(b);
    return toResponse(b);
  }

  @Transactional
  public BeneficiaryResponse update(UUID customerId, UUID id, BeneficiaryRequest request) {
    Beneficiary b = find(customerId, id);
    b.alias = request.alias();
    b.bankName = request.bankName();
    b.accountNumber = request.accountNumber();
    b.currency = request.currency();
    return toResponse(b);
  }

  @Transactional
  public void deactivate(UUID customerId, UUID id) {
    find(customerId, id).status = BeneficiaryStatus.BLOCKED;
  }

  public Beneficiary find(UUID customerId, UUID id) {
    Beneficiary b = beneficiaries.find("id = ?1 and customerId = ?2", id, customerId).firstResult();
    if (b == null)
      throw new ApiException(
          Response.Status.NOT_FOUND, "BENEFICIARY_NOT_FOUND", "Beneficiary not found");
    return b;
  }

  private BeneficiaryResponse toResponse(Beneficiary b) {
    return new BeneficiaryResponse(
        b.id,
        b.customerId,
        b.alias,
        b.bankName,
        b.accountNumber,
        b.currency,
        b.status.name(),
        b.createdAt);
  }
}
