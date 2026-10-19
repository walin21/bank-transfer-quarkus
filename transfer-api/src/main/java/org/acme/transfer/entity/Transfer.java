package org.acme.transfer.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer")
public class Transfer extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "customer_id", nullable = false)
  public UUID customerId;

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @jakarta.persistence.JoinColumn(name = "customer_id", insertable = false, updatable = false)
  public Customer customer;

  @Column(name = "source_account_id", nullable = false)
  public UUID sourceAccountId;

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @jakarta.persistence.JoinColumn(name = "source_account_id", insertable = false, updatable = false)
  public Account sourceAccount;

  @Column(name = "beneficiary_id", nullable = false)
  public UUID beneficiaryId;

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @jakarta.persistence.JoinColumn(name = "beneficiary_id", insertable = false, updatable = false)
  public Beneficiary beneficiary;

  @Column(nullable = false, precision = 19, scale = 4)
  public BigDecimal amount;

  @Column(name = "source_currency", nullable = false, length = 3)
  public String sourceCurrency;

  @Column(name = "target_currency", nullable = false, length = 3)
  public String targetCurrency;

  @Column(name = "exchange_rate", precision = 19, scale = 10)
  public BigDecimal exchangeRate;

  @Column(name = "converted_amount", nullable = false, precision = 19, scale = 4)
  public BigDecimal convertedAmount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  public TransferStatus status;

  @Column(name = "failure_reason", length = 500)
  public String failureReason;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "processed_at")
  public Instant processedAt;
}
