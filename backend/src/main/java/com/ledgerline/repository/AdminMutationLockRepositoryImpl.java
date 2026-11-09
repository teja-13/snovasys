package com.ledgerline.repository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
class AdminMutationLockRepositoryImpl implements AdminMutationLockRepository {

    private static final long ADMIN_MUTATION_LOCK_KEY = 78123456789012345L;

    private final EntityManager entityManager;

    AdminMutationLockRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void acquireAdminMutationLock() {
        // The transaction-scoped PostgreSQL lock serializes bootstrap and last-admin mutations.
        // EntityManager keeps lock acquisition on the transaction's connection.
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockKey)")
                .setParameter("lockKey", ADMIN_MUTATION_LOCK_KEY)
                .getSingleResult();
    }
}
