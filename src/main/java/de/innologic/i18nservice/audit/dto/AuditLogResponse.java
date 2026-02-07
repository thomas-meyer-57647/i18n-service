package de.innologic.i18nservice.audit.dto;

import de.innologic.i18nservice.audit.model.AuditLogEntity;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Instant occurredAt,
        String requestId,
        String actor,
        String action,
        String entityType,
        String entityKey,
        String detailsJson
) {
    public static AuditLogResponse from(AuditLogEntity e) {
        if (e == null) return null;

        return new AuditLogResponse(
                e.getId(),
                e.getOccurredAt(),
                e.getRequestId(),
                e.getActor(),
                e.getAction(),
                e.getEntityType(),
                e.getEntityKey(),
                e.getDetailsJson()
        );
    }
}
