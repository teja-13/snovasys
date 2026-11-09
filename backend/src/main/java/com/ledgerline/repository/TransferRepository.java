package com.ledgerline.repository;

import com.ledgerline.domain.Transfer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transfer t where t.initiator.id = :initiatorId and t.idempotencyKey = :idempotencyKey")
    Optional<Transfer> findByInitiatorIdAndIdempotencyKeyForUpdate(
            UUID initiatorId, String idempotencyKey);
}
