package com.ledgerline.audit;

import java.time.Instant;

public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditLogResponse(
            String actorEmail,
            String action,
            String resourceType,
            String resourceId,
            Instant createdAt,
            String details) {
    }
}
