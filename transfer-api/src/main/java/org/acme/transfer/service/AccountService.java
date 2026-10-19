package org.acme.transfer.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import org.acme.transfer.dto.response.AccountResponse;
import org.acme.transfer.entity.Account;
import org.acme.transfer.repository.AccountRepository;

@ApplicationScoped
public class AccountService {
  @Inject AccountRepository accounts;

  public List<AccountResponse> findByCustomer(UUID customerId) {
    return accounts.find("customerId = ?1 order by createdAt", customerId).stream()
        .map(this::toResponse)
        .toList();
  }

  private AccountResponse toResponse(Account a) {
    return new AccountResponse(
        a.id, a.customerId, a.accountNumber, a.currency, a.balance, a.status.name(), a.createdAt);
  }
}
