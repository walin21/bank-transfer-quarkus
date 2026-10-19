package org.acme.transfer.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import org.acme.transfer.entity.Account;
import org.acme.transfer.entity.AccountStatus;
import org.acme.transfer.entity.Beneficiary;
import org.acme.transfer.entity.BeneficiaryStatus;
import org.acme.transfer.entity.Transfer;
import org.acme.transfer.entity.TransferHistory;
import org.acme.transfer.entity.TransferStatus;
import org.acme.transfer.event.TransferCreated;
import org.acme.transfer.repository.AccountRepository;
import org.acme.transfer.repository.BeneficiaryRepository;
import org.acme.transfer.repository.CustomerRepository;
import org.acme.transfer.repository.TransferHistoryRepository;
import org.acme.transfer.repository.TransferRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TransferProcessingService {
  private static final Logger LOG = Logger.getLogger(TransferProcessingService.class);

  @Inject TransferRepository transfers;
  @Inject AccountRepository accounts;
  @Inject BeneficiaryRepository beneficiaries;
  @Inject CustomerRepository customers;
  @Inject TransferHistoryRepository history;

  @Transactional
  public void process(TransferCreated event) {
    UUID transferId = parseUuid(event.getTransferId(), "transferId");
    Transfer transfer =
        transfers.find("id", transferId).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();

    if (transfer == null) {
      // The API can publish the event before its database transaction commits.
      // This is a technical/transient failure, not a business rejection: by
      // throwing, the messaging connector does not acknowledge the record and
      // Kafka can retry it after the transfer becomes visible.
      LOG.errorf(
          "Transfer %s from event %s is not visible yet; retrying event",
          transferId, event.getEventId());
      throw new TransferNotVisibleException(transferId);
    }

    // The row lock makes this check and the debit atomic when the same event is redelivered.
    if (transfer.status == TransferStatus.COMPLETED || transfer.status == TransferStatus.REJECTED) {
      LOG.infof(
          "Ignoring duplicate event %s for terminal transfer %s", event.getEventId(), transferId);
      return;
    }

    try {
      validateEvent(event, transfer);
      CustomerReference customer = validateCustomer(event, transfer);
      Beneficiary beneficiary = validateBeneficiary(event, transfer);
      Account account =
          accounts
              .find("id", transfer.sourceAccountId)
              .withLock(LockModeType.PESSIMISTIC_WRITE)
              .firstResult();
      validateAccount(event, transfer, account, customer, beneficiary);

      // In this simulated system the beneficiary account number identifies
      // an internal destination account. Both movements are part of this
      // transaction; if the credit cannot be made, the debit is rolled back.
      Account destination =
          accounts
              .find("accountNumber", beneficiary.accountNumber)
              .withLock(LockModeType.PESSIMISTIC_WRITE)
              .firstResult();
      validateDestination(event, beneficiary, destination);

      BigDecimal amount = decimal(event.getAmount(), 2);
      if (account.balance.compareTo(amount) < 0) {
        throw new BusinessRejection("INSUFFICIENT_FUNDS", "Insufficient source account balance");
      }

      account.balance = account.balance.subtract(amount);
      BigDecimal convertedAmount = transfer.convertedAmount;
      if (convertedAmount == null || convertedAmount.signum() <= 0) {
        BigDecimal rate = transfer.exchangeRate == null ? BigDecimal.ONE : transfer.exchangeRate;
        convertedAmount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
      }
      destination.balance = destination.balance.add(convertedAmount);
      transfer.status = TransferStatus.COMPLETED;
      transfer.failureReason = null;
      transfer.processedAt = Instant.now();
      addHistory(transfer, TransferStatus.COMPLETED, "Transfer processed successfully");
      LOG.infof(
          "Transfer %s completed; debited %s %s and credited %s %s",
          transfer.id, amount, account.currency, convertedAmount, destination.currency);
    } catch (BusinessRejection rejection) {
      reject(transfer, rejection.reason());
      LOG.warnf("Transfer %s rejected: %s", transfer.id, rejection.reason());
    }
  }

  private void validateEvent(TransferCreated event, Transfer transfer) {
    requireSame("customerId", event.getCustomerId(), transfer.customerId);
    requireSame("sourceAccountId", event.getSourceAccountId(), transfer.sourceAccountId);
    requireSame("beneficiaryId", event.getBeneficiaryId(), transfer.beneficiaryId);
    if (transfer.amount.compareTo(decimal(event.getAmount(), 2)) != 0) {
      throw new BusinessRejection(
          "EVENT_AMOUNT_MISMATCH", "Event amount does not match the transfer");
    }
    if (!transfer.sourceCurrency.equals(event.getSourceCurrency())
        || !transfer.targetCurrency.equals(event.getTargetCurrency())) {
      throw new BusinessRejection(
          "EVENT_CURRENCY_MISMATCH", "Event currencies do not match the transfer");
    }
  }

  private CustomerReference validateCustomer(TransferCreated event, Transfer transfer) {
    UUID customerId = parseUuid(event.getCustomerId(), "customerId");
    if (!customerId.equals(transfer.customerId)
        || customers.find("id", customerId).firstResult() == null) {
      throw new BusinessRejection("CUSTOMER_NOT_FOUND", "Transfer customer does not exist");
    }
    return new CustomerReference(customerId);
  }

  private Beneficiary validateBeneficiary(TransferCreated event, Transfer transfer) {
    UUID beneficiaryId = parseUuid(event.getBeneficiaryId(), "beneficiaryId");
    Beneficiary beneficiary = beneficiaries.find("id", beneficiaryId).firstResult();
    if (beneficiary == null || beneficiary.status != BeneficiaryStatus.ACTIVE) {
      throw new BusinessRejection("BENEFICIARY_NOT_ACTIVE", "Beneficiary is not active");
    }
    if (!beneficiary.customerId.equals(transfer.customerId)) {
      throw new BusinessRejection(
          "BENEFICIARY_OWNER_MISMATCH", "Beneficiary does not belong to the transfer customer");
    }
    return beneficiary;
  }

  private void validateAccount(
      TransferCreated event,
      Transfer transfer,
      Account account,
      CustomerReference customer,
      Beneficiary beneficiary) {
    if (account == null) {
      throw new BusinessRejection("SOURCE_ACCOUNT_NOT_FOUND", "Source account does not exist");
    }
    if (account.status != AccountStatus.ACTIVE) {
      throw new BusinessRejection("ACCOUNT_NOT_ACTIVE", "Source account is not active");
    }
    if (!account.customerId.equals(customer.id())
        || !account.currency.equals(event.getSourceCurrency())) {
      throw new BusinessRejection(
          "SOURCE_ACCOUNT_MISMATCH", "Source account does not match the event");
    }
    if (!beneficiary.currency.equals(event.getTargetCurrency())) {
      throw new BusinessRejection(
          "BENEFICIARY_CURRENCY_MISMATCH", "Beneficiary currency does not match the event");
    }
  }

  private void validateDestination(
      TransferCreated event, Beneficiary beneficiary, Account destination) {
    if (destination == null) {
      throw new BusinessRejection(
          "DESTINATION_ACCOUNT_NOT_FOUND", "Beneficiary destination account does not exist");
    }
    if (destination.status != AccountStatus.ACTIVE) {
      throw new BusinessRejection(
          "DESTINATION_ACCOUNT_NOT_ACTIVE", "Beneficiary destination account is not active");
    }
    if (!destination.currency.equals(event.getTargetCurrency())) {
      throw new BusinessRejection(
          "DESTINATION_ACCOUNT_CURRENCY_MISMATCH",
          "Destination account currency does not match the event");
    }
  }

  private void reject(Transfer transfer, String reason) {
    transfer.status = TransferStatus.REJECTED;
    transfer.failureReason = reason;
    transfer.processedAt = Instant.now();
    addHistory(transfer, TransferStatus.REJECTED, reason);
  }

  private void addHistory(Transfer transfer, TransferStatus newStatus, String reason) {
    TransferHistory entry = new TransferHistory();
    entry.id = UUID.randomUUID();
    entry.transferId = transfer.id;
    entry.previousStatus = transfer.status == newStatus ? TransferStatus.PENDING : transfer.status;
    entry.newStatus = newStatus;
    entry.reason = reason;
    entry.createdAt = Instant.now();
    history.persist(entry);
  }

  private static void requireSame(String field, String value, UUID expected) {
    if (!expected.equals(parseUuid(value, field))) {
      throw new BusinessRejection(
          "EVENT_" + field.toUpperCase() + "_MISMATCH",
          "Event " + field + " does not match the transfer");
    }
  }

  private static UUID parseUuid(String value, String field) {
    try {
      return UUID.fromString(value);
    } catch (RuntimeException exception) {
      throw new BusinessRejection(
          "INVALID_" + field.toUpperCase(), "Event contains an invalid " + field);
    }
  }

  private static BigDecimal decimal(BigDecimal value, int scale) {
    if (value == null) {
      throw new BusinessRejection("INVALID_DECIMAL", "Event contains a missing decimal value");
    }
    return value.setScale(scale, RoundingMode.HALF_UP);
  }

  private record CustomerReference(UUID id) {}

  private static final class BusinessRejection extends RuntimeException {
    private final String code;

    private BusinessRejection(String code, String reason) {
      super(reason);
      this.code = code;
    }

    private String reason() {
      return getMessage();
    }
  }

  private static final class TransferNotVisibleException extends RuntimeException {
    private TransferNotVisibleException(UUID transferId) {
      super("Transfer is not visible yet: " + transferId);
    }
  }
}
