package org.acme.transfer.messaging;

import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.transfer.event.TransferCreated;
import org.eclipse.microprofile.reactive.messaging.Channel;

@ApplicationScoped
public class TransferCreatedProducer {
  @Inject
  @Channel("transfer-created")
  MutinyEmitter<TransferCreated> publisher;

  public void publish(TransferCreated event) {
    publisher.send(event).await().indefinitely();
  }
}
