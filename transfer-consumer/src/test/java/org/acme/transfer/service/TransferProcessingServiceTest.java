package org.acme.transfer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.acme.transfer.entity.Account;
import org.acme.transfer.entity.AccountStatus;
import org.acme.transfer.entity.Beneficiary;
import org.acme.transfer.entity.BeneficiaryStatus;
import org.acme.transfer.entity.Customer;
import org.acme.transfer.entity.Transfer;
import org.acme.transfer.entity.TransferHistory;
import org.acme.transfer.entity.TransferStatus;
import org.acme.transfer.event.TransferCreated;
import org.acme.transfer.repository.AccountRepository;
import org.acme.transfer.repository.BeneficiaryRepository;
import org.acme.transfer.repository.CustomerRepository;
import org.acme.transfer.repository.TransferHistoryRepository;
import org.acme.transfer.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TransferProcessingServiceTest {
  @InjectMock TransferRepository transfers;
  @InjectMock AccountRepository accounts;
  @InjectMock BeneficiaryRepository beneficiaries;
  @InjectMock CustomerRepository customers;
  @InjectMock TransferHistoryRepository history;
  @jakarta.inject.Inject TransferProcessingService processor;

  private UUID customerId;
  private UUID accountId;
  private UUID beneficiaryId;
  private UUID transferId;
  private Transfer transfer;
  private Account account;
  private Account destination;
  private Beneficiary beneficiary;

  @BeforeEach
  void setUp() {
    customerId = UUID.randomUUID();
    accountId = UUID.randomUUID();
    beneficiaryId = UUID.randomUUID();
    transferId = UUID.randomUUID();

    transfer = new Transfer();
    transfer.id = transferId;
    transfer.customerId = customerId;
    transfer.sourceAccountId = accountId;
    transfer.beneficiaryId = beneficiaryId;
    transfer.amount = new BigDecimal("25.50");
    transfer.sourceCurrency = "PEN";
    transfer.targetCurrency = "PEN";
    transfer.convertedAmount = new BigDecimal("25.50");
    transfer.status = TransferStatus.PENDING;

    account = new Account();
    account.id = accountId;
    account.customerId = customerId;
    account.currency = "PEN";
    account.balance = new BigDecimal("100.00");
    account.status = AccountStatus.ACTIVE;

    beneficiary = new Beneficiary();
    beneficiary.id = beneficiaryId;
    beneficiary.customerId = customerId;
    beneficiary.currency = "PEN";
    beneficiary.accountNumber = "destination-001";
    beneficiary.status = BeneficiaryStatus.ACTIVE;

    PanacheQuery<Transfer> transferQuery = org.mockito.Mockito.mock(PanacheQuery.class);
    when(transfers.find("id", transferId)).thenReturn(transferQuery);
    when(transferQuery.withLock(any())).thenReturn(transferQuery);
    when(transferQuery.firstResult()).thenReturn(transfer);
    PanacheQuery<Customer> customerQuery = queryOf(new Customer());
    PanacheQuery<Beneficiary> beneficiaryQuery = queryOf(beneficiary);
    when(customers.find("id", customerId)).thenReturn(customerQuery);
    when(beneficiaries.find("id", beneficiaryId)).thenReturn(beneficiaryQuery);
    PanacheQuery<Account> accountQuery = org.mockito.Mockito.mock(PanacheQuery.class);
    when(accounts.find("id", accountId)).thenReturn(accountQuery);
    when(accountQuery.withLock(any())).thenReturn(accountQuery);
    when(accountQuery.firstResult()).thenReturn(account);
    destination = new Account();
    destination.id = UUID.randomUUID();
    destination.currency = "PEN";
    destination.balance = new BigDecimal("10.00");
    destination.status = AccountStatus.ACTIVE;
    PanacheQuery<Account> destinationQuery = queryOf(destination);
    when(accounts.find("accountNumber", beneficiary.accountNumber)).thenReturn(destinationQuery);
    doNothing().when(history).persist(any(TransferHistory.class));
  }

  @Test
  void completesAndDebitsOnlyOnceWhenEventIsRedelivered() {
    processor.process(event());
    assertEquals(TransferStatus.COMPLETED, transfer.status);
    assertEquals(new BigDecimal("74.50"), account.balance);
    assertEquals(new BigDecimal("35.50"), destination.balance);

    processor.process(event());
    assertEquals(new BigDecimal("74.50"), account.balance);
    assertEquals(new BigDecimal("35.50"), destination.balance);
    verify(history).persist(any(TransferHistory.class));
  }

  @Test
  void rejectsInsufficientFundsWithoutChangingBalance() {
    account.balance = new BigDecimal("10.00");

    processor.process(event());

    assertEquals(TransferStatus.REJECTED, transfer.status);
    assertEquals("10.00", account.balance.toPlainString());
    assertEquals("Insufficient source account balance", transfer.failureReason);
  }

  @Test
  void throwsTechnicalErrorWhenTransferIsNotVisibleYet() {
    when(transfers.find("id", transferId).withLock(any()).firstResult()).thenReturn(null);

    assertThrows(RuntimeException.class, () -> processor.process(event()));
    verify(history, org.mockito.Mockito.never()).persist(any(TransferHistory.class));
  }

  private TransferCreated event() {
    return new TransferCreated(
        UUID.randomUUID().toString(),
        transferId.toString(),
        customerId.toString(),
        accountId.toString(),
        beneficiaryId.toString(),
        new BigDecimal("25.50"),
        "PEN",
        "PEN",
        null,
        Instant.now());
  }

  private static <T> PanacheQuery<T> queryOf(T value) {
    PanacheQuery<T> query = org.mockito.Mockito.mock(PanacheQuery.class);
    when(query.withLock(any())).thenReturn(query);
    when(query.firstResult()).thenReturn(value);
    return query;
  }
}
