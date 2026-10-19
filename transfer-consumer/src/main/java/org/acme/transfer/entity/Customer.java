package org.acme.transfer.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer")
public class Customer extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "document_number", nullable = false, unique = true, length = 32)
  public String documentNumber;

  @Column(name = "full_name", nullable = false, length = 160)
  public String fullName;

  @Column(nullable = false, unique = true)
  public String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  public CustomerStatus status;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
