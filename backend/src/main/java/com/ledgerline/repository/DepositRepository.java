package com.ledgerline.repository;

import com.ledgerline.domain.Deposit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface DepositRepository extends JpaRepository<Deposit, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Deposit d where d.initiator.id = :initiatorId and d.idempotencyKey = :idempotencyKey")
    Optional<Deposit> findByInitiatorIdAndIdempotencyKeyForUpdate(
            UUID initiatorId, String idempotencyKey);

    @Query("""
            select d from Deposit d
            where d.account.owner.id = :userId
            order by d.createdAt desc
            """)
    List<Deposit> findHistoryForUser(UUID userId);

    @Query("select d from Deposit d order by d.createdAt desc")
    List<Deposit> findAllHistory();
}
