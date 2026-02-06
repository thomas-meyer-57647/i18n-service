package de.innologic.i18nservice.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.i18nservice.audit.model.AuditLogEntity;
import de.innologic.i18nservice.audit.repo.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditLogService {

    private final AuditLogRepository repo;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    /**
     * Audit soll Business-Operationen nicht kaputt machen.
     * Darum: JSON-Fehler werden abgefangen und als kleiner Fallback gespeichert.
     */
    public void log(String projectKey,
                    String actor,
                    String action,
                    String entityType,
                    String entityKey,
                    Object details,
                    String requestId) {

        AuditLogEntity e = new AuditLogEntity();
        e.setOccurredAt(Instant.now());
        e.setProjectKey(projectKey);
        e.setActor(actor);
        e.setAction(action);
        e.setEntityType(entityType);
        e.setEntityKey(entityKey);
        e.setRequestId(requestId);

        if (details != null) {
            e.setDetailsJson(toJsonSafe(details));
        }

        repo.save(e);
    }

    private String toJsonSafe(Object details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception ex) {
            // super kleiner Fallback
            return "{\"_error\":\"details_not_serializable\",\"type\":\"" + details.getClass().getName() + "\"}";
        }
    }
}
