package org.acme.transfer.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import org.acme.transfer.entity.TransferHistory;

@ApplicationScoped
public class TransferHistoryRepository implements PanacheRepositoryBase<TransferHistory, UUID> {}
