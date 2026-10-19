package org.acme.transfer.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TransferHistoryResponse(
    UUID id,
    UUID transferId,
    String previousStatus,
    String newStatus,
    String reason,
    Instant createdAt) {}
