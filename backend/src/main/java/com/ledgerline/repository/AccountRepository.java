package com.ledgerline.repository;

import com.ledgerline.domain.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findAllByOwnerIdOrderByCreatedAtAsc(UUID ownerId);

    long countByOwnerId(UUID ownerId);

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);
}
