package org.acme.transfer.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.transfer.event.TransferCreated;
import org.acme.transfer.service.TransferProcessingService;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class TransferCreatedConsumer {
  @Inject TransferProcessingService processor;

  @Incoming("transfer-created")
  public void consume(TransferCreated event) {
    // Returning only after the transaction commits makes Reactive Messaging acknowledge the offset.
    processor.process(event);
  }
}
