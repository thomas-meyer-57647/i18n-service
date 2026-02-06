package de.innologic.i18nservice.audit.dto;

import de.innologic.i18nservice.audit.model.AuditLogEntity;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Instant occurredAt,
        String actor,
        String action,
        String entityType,
        String entityKey,
        String detailsJson
) {
    public static AuditLogResponse from(AuditLogEntity e) {
        return new AuditLogResponse(
                e.getId(),
                e.getOccurredAt(),
                e.getActor(),
                e.getAction(),
                e.getEntityType(),
                e.getEntityKey(),
                e.getDetailsJson()
        );
    }
}
