package org.acme.transfer.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "beneficiary")
public class Beneficiary extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "customer_id", nullable = false)
  public UUID customerId;

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @jakarta.persistence.JoinColumn(name = "customer_id", insertable = false, updatable = false)
  public Customer customer;

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @jakarta.persistence.JoinColumn(
      name = "account_number",
      referencedColumnName = "account_number",
      insertable = false,
      updatable = false,
      foreignKey =
          @jakarta.persistence.ForeignKey(value = jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
  public Account destinationAccount;

  @Column(nullable = false, length = 80)
  public String alias;

  @Column(name = "bank_name", nullable = false, length = 120)
  public String bankName;

  @Column(name = "account_number", nullable = false, length = 32)
  public String accountNumber;

  @Column(nullable = false, length = 3)
  public String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  public BeneficiaryStatus status;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
