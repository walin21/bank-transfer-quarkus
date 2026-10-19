package org.acme.transfer.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account")
public class Account extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "customer_id", nullable = false)
  public UUID customerId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", insertable = false, updatable = false)
  public Customer customer;

  @Column(name = "account_number", nullable = false, unique = true, length = 32)
  public String accountNumber;

  @Column(nullable = false, length = 3)
  public String currency;

  @Column(nullable = false, precision = 19, scale = 4)
  public BigDecimal balance;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  public AccountStatus status;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
