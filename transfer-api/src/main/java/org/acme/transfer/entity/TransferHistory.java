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
@Table(name = "transfer_history")
public class TransferHistory extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "transfer_id", nullable = false)
  public UUID transferId;

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @jakarta.persistence.JoinColumn(name = "transfer_id", insertable = false, updatable = false)
  public Transfer transfer;

  @Enumerated(EnumType.STRING)
  @Column(name = "previous_status", length = 16)
  public TransferStatus previousStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false, length = 16)
  public TransferStatus newStatus;

  @Column(nullable = false, length = 500)
  public String reason;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;
}
