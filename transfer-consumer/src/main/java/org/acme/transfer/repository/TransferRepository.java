package org.acme.transfer.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import org.acme.transfer.entity.Transfer;

@ApplicationScoped
public class TransferRepository implements PanacheRepositoryBase<Transfer, UUID> {}
