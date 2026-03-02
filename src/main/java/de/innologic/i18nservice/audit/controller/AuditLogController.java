package de.innologic.i18nservice.audit.controller;

import de.innologic.i18nservice.audit.dto.AuditLogResponse;
import de.innologic.i18nservice.audit.repo.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/{projectKey}/audit-logs")
@Tag(name = "Audit Logs", description = "Read structured audit log entries")
public class AuditLogController {

    private final AuditLogRepository repo;

    public AuditLogController(AuditLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @Operation(
            summary = "List audit logs",
            description = "Returns the most recent audit log entries for the project.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Audit Logs"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit logs returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuditLogResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public List<AuditLogResponse> list(
            @PathVariable @NotBlank String projectKey,
            @RequestParam(name = "limit", defaultValue = "100")
            @Parameter(description = "Maximum number of log entries to return (1-500)", example = "100", in = ParameterIn.QUERY) int limit,
            @RequestParam(name = "entityType", required = false)
            @Parameter(description = "Filter by audited entity type", example = "Language", in = ParameterIn.QUERY) String entityType,
            @RequestParam(name = "entityKey", required = false)
            @Parameter(description = "Further filter by entity key", example = "de-DE", in = ParameterIn.QUERY) String entityKey
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
