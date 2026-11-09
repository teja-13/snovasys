package com.ledgerline.repository;

import com.ledgerline.domain.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface AppUserRepository extends JpaRepository<AppUser, UUID>, AdminMutationLockRepository {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    @Query("select distinct u from AppUser u left join fetch u.roles where u.id = :id")
    Optional<AppUser> findByIdWithRoles(UUID id);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select count(distinct u) from AppUser u
            join u.roles r
            where r.name = :roleName
            """)
    long countByRole(com.ledgerline.domain.RoleName roleName);

    @Query("""
            select count(distinct u) from AppUser u
            join u.roles r
            where r.name = :roleName and u.active = true
            """)
    long countActiveByRole(com.ledgerline.domain.RoleName roleName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where lower(u.email) = lower(:email)")
    Optional<AppUser> findByEmailIgnoreCaseForUpdate(String email);
}
