package de.innologic.i18nservice.audit.dto;

import de.innologic.i18nservice.audit.model.AuditLogEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Audit log entry details")
public record AuditLogResponse(
        @Schema(description = "Database identifier for the log entry", example = "42") Long id,
        @Schema(description = "Timestamp when the action occurred", example = "2026-03-02T13:45:30Z") Instant occurredAt,
        @Schema(description = "X-Request-Id that originated the action", example = "req-abc-123") String requestId,
        @Schema(description = "Actor (user/service) that performed the action", example = "system") String actor,
        @Schema(description = "Action type stored in the audit log", example = "language.create") String action,
        @Schema(description = "Type of entity affected by the action", example = "Language") String entityType,
        @Schema(description = "Key of the entity that was touched", example = "de-DE") String entityKey,
        @Schema(description = "Raw JSON payload describing the event", example = "{\"changes\":\"...\"}") String detailsJson
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
