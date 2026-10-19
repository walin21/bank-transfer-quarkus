package org.acme.transfer.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import org.acme.transfer.client.ExchangeRateProvider;
import org.acme.transfer.dto.request.TransferRequest;
import org.acme.transfer.dto.response.TransferResponse;
import org.acme.transfer.entity.Account;
import org.acme.transfer.entity.AccountStatus;
import org.acme.transfer.entity.Beneficiary;
import org.acme.transfer.entity.BeneficiaryStatus;
import org.acme.transfer.entity.Transfer;
import org.acme.transfer.entity.TransferHistory;
import org.acme.transfer.entity.TransferStatus;
import org.acme.transfer.event.TransferCreated;
import org.acme.transfer.exception.ApiException;
import org.acme.transfer.repository.AccountRepository;
import org.acme.transfer.repository.BeneficiaryRepository;
import org.acme.transfer.repository.TransferHistoryRepository;
import org.acme.transfer.repository.TransferRepository;

@ApplicationScoped
public class TransferRegistrationService {
  @Inject AccountRepository accounts;
  @Inject BeneficiaryRepository beneficiaries;
  @Inject TransferRepository transfers;
  @Inject TransferHistoryRepository history;
  @Inject ExchangeRateProvider exchangeRates;

  @Transactional
  public Registration register(UUID customerId, TransferRequest request) {
    Account account =
        accounts
            .find("id = ?1 and customerId = ?2", request.sourceAccountId(), customerId)
            .firstResult();
    if (account == null)
      throw new ApiException(
          Response.Status.NOT_FOUND, "SOURCE_ACCOUNT_NOT_FOUND", "Source account not found");
    if (account.status != AccountStatus.ACTIVE)
      throw new ApiException(
          Response.Status.CONFLICT, "ACCOUNT_BLOCKED", "Source account is not active");

    Beneficiary beneficiary =
        beneficiaries
            .find("id = ?1 and customerId = ?2", request.beneficiaryId(), customerId)
            .firstResult();
    if (beneficiary == null || beneficiary.status != BeneficiaryStatus.ACTIVE)
      throw new ApiException(
          Response.Status.NOT_FOUND, "BENEFICIARY_NOT_FOUND", "Beneficiary not found");

    BigDecimal rate = null;
    BigDecimal conversionRate = BigDecimal.ONE;
    if (!account.currency.equals(beneficiary.currency)) {
      try {
        rate = exchangeRates.getRate(account.currency, beneficiary.currency);
      } catch (RuntimeException e) {
        throw new ApiException(
            Response.Status.SERVICE_UNAVAILABLE,
            "EXCHANGE_RATE_UNAVAILABLE",
            "Exchange-rate service is unavailable");
      }
      if (rate == null || rate.signum() <= 0)
        throw new ApiException(
            Response.Status.SERVICE_UNAVAILABLE,
            "EXCHANGE_RATE_UNAVAILABLE",
            "Exchange-rate service returned an invalid rate");
      rate = rate.setScale(10, RoundingMode.HALF_UP);
      conversionRate = rate;
    }

    BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
    Transfer transfer = new Transfer();
    transfer.id = UUID.randomUUID();
    transfer.customerId = customerId;
    transfer.sourceAccountId = account.id;
    transfer.beneficiaryId = beneficiary.id;
    transfer.amount = amount;
    transfer.sourceCurrency = account.currency;
    transfer.targetCurrency = beneficiary.currency;
    transfer.exchangeRate = rate;
    transfer.convertedAmount = amount.multiply(conversionRate).setScale(2, RoundingMode.HALF_UP);
    transfer.status = TransferStatus.PENDING;
    transfer.createdAt = Instant.now();
    transfers.persist(transfer);

    TransferHistory historyEntry = new TransferHistory();
    historyEntry.id = UUID.randomUUID();
    historyEntry.transferId = transfer.id;
    historyEntry.newStatus = TransferStatus.PENDING;
    historyEntry.reason = "Transfer created";
    historyEntry.createdAt = transfer.createdAt;
    history.persist(historyEntry);

    TransferCreated event =
        new TransferCreated(
            UUID.randomUUID().toString(),
            transfer.id.toString(),
            customerId.toString(),
            transfer.sourceAccountId.toString(),
            transfer.beneficiaryId.toString(),
            amount,
            transfer.sourceCurrency,
            transfer.targetCurrency,
            transfer.exchangeRate,
            transfer.createdAt);
    return new Registration(toResponse(transfer), event);
  }

  private TransferResponse toResponse(Transfer transfer) {
    return new TransferResponse(
        transfer.id,
        transfer.customerId,
        transfer.sourceAccountId,
        transfer.beneficiaryId,
        transfer.amount,
        transfer.sourceCurrency,
        transfer.targetCurrency,
        transfer.exchangeRate,
        transfer.convertedAmount,
        transfer.status.name(),
        transfer.failureReason,
        transfer.createdAt,
        transfer.processedAt);
  }

  public record Registration(TransferResponse response, TransferCreated event) {}
}
