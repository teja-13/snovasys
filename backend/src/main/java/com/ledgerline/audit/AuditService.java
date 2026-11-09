package com.ledgerline.audit;

import com.ledgerline.domain.AuditLog;
import com.ledgerline.repository.AuditLogRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditDtos.AuditLogResponse> findAll() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    private AuditDtos.AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditDtos.AuditLogResponse(
                auditLog.getActor() == null ? null : auditLog.getActor().getEmail(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getCreatedAt(),
                auditLog.getDetails());
    }
}
