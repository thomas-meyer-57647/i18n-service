package de.innologic.i18nservice.audit.controller;

import de.innologic.i18nservice.audit.dto.AuditLogResponse;
import de.innologic.i18nservice.audit.repo.AuditLogRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/{projectKey}/audit-logs")
public class AuditLogController {

    private final AuditLogRepository repo;

    public AuditLogController(AuditLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<AuditLogResponse> list(
            @PathVariable @NotBlank String projectKey,
            @RequestParam(name = "limit", defaultValue = "100") int limit,
            @RequestParam(name = "entityType", required = false) String entityType,
            @RequestParam(name = "entityKey", required = false) String entityKey
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 500);

        var pageable = PageRequest.of(0, safeLimit);

        if (entityType != null && entityKey != null) {
            return repo.findByProjectKeyAndEntityTypeAndEntityKeyOrderByOccurredAtDesc(projectKey, entityType, entityKey, pageable)
                    .map(AuditLogResponse::from)
                    .toList();
        }

        return repo.findByProjectKeyOrderByOccurredAtDesc(projectKey, pageable)
                .map(AuditLogResponse::from)
                .toList();
    }
}
