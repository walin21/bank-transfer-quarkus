package org.acme.transfer.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_history")
public class TransferHistory extends PanacheEntityBase {
  @Id public UUID id;

  @Column(name = "transfer_id", nullable = false)
  public UUID transferId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transfer_id", insertable = false, updatable = false)
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
