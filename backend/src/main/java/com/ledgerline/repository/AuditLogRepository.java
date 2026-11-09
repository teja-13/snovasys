package com.ledgerline.repository;

import com.ledgerline.domain.AuditLog;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
