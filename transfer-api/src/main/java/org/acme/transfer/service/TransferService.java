package org.acme.transfer.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.acme.transfer.dto.request.TransferRequest;
import org.acme.transfer.dto.response.TransferHistoryResponse;
import org.acme.transfer.dto.response.TransferResponse;
import org.acme.transfer.entity.Transfer;
import org.acme.transfer.entity.TransferStatus;
import org.acme.transfer.exception.ApiException;
import org.acme.transfer.messaging.TransferCreatedProducer;
import org.acme.transfer.repository.TransferHistoryRepository;
import org.acme.transfer.repository.TransferRepository;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TransferService {
  private static final Logger LOG = Logger.getLogger(TransferService.class);

  @Inject TransferRepository transfers;
  @Inject TransferHistoryRepository history;
  @Inject TransferRegistrationService registration;
  @Inject TransferCreatedProducer producer;

  /** Persists and commits first; Kafka publication only happens after register returns. */
  public TransferResponse create(UUID customerId, TransferRequest request) {
    TransferRegistrationService.Registration created = registration.register(customerId, request);
    try {
      producer.publish(created.event());
    } catch (RuntimeException e) {
      throw new ApiException(
          Response.Status.SERVICE_UNAVAILABLE,
          "EVENT_PUBLICATION_FAILED",
          "Transfer event could not be published");
    }
    LOG.infof(
        "Published TransferCreated event %s for transfer %s",
        created.event().getEventId(), created.response().id());
    return created.response();
  }

  public TransferResponse get(UUID customerId, UUID id) {
    return toResponse(find(customerId, id));
  }

  public List<TransferResponse> list(UUID customerId) {
    return transfers.find("customerId = ?1 order by createdAt desc", customerId).stream()
        .map(this::toResponse)
        .toList();
  }

  public List<TransferHistoryResponse> history(UUID customerId, UUID transferId) {
    find(customerId, transferId);
    return history.find("transferId = ?1 order by createdAt", transferId).stream()
        .map(
            h ->
                new TransferHistoryResponse(
                    h.id,
                    h.transferId,
                    h.previousStatus == null ? null : h.previousStatus.name(),
                    h.newStatus.name(),
                    h.reason,
                    h.createdAt))
        .toList();
  }

  public List<TransferResponse> adminList(
      String status, UUID customerId, Instant from, Instant to) {
    StringBuilder query = new StringBuilder("1 = 1");
    List<Object> parameters = new java.util.ArrayList<>();
    if (status != null) {
      query.append(" and status = ?").append(parameters.size() + 1);
      parameters.add(parseStatus(status));
    }
    if (customerId != null) {
      query.append(" and customerId = ?").append(parameters.size() + 1);
      parameters.add(customerId);
    }
    if (from != null) {
      query.append(" and createdAt >= ?").append(parameters.size() + 1);
      parameters.add(from);
    }
    if (to != null) {
      query.append(" and createdAt <= ?").append(parameters.size() + 1);
      parameters.add(to);
    }
    query.append(" order by createdAt desc");
    return transfers.find(query.toString(), parameters.toArray()).list().stream()
        .map(this::toResponse)
        .toList();
  }

  private TransferStatus parseStatus(String value) {
    try {
      return TransferStatus.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ApiException(
          Response.Status.BAD_REQUEST, "INVALID_TRANSFER_STATUS", "Invalid transfer status");
    }
  }

  private Transfer find(UUID customerId, UUID id) {
    Transfer t = transfers.find("id = ?1 and customerId = ?2", id, customerId).firstResult();
    if (t == null)
      throw new ApiException(Response.Status.NOT_FOUND, "TRANSFER_NOT_FOUND", "Transfer not found");
    return t;
  }

  private TransferResponse toResponse(Transfer t) {
    return new TransferResponse(
        t.id,
        t.customerId,
        t.sourceAccountId,
        t.beneficiaryId,
        t.amount,
        t.sourceCurrency,
        t.targetCurrency,
        t.exchangeRate,
        t.convertedAmount,
        t.status.name(),
        t.failureReason,
        t.createdAt,
        t.processedAt);
  }
}
