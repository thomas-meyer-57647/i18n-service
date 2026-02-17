package de.innologic.i18nservice.project.service;

import de.innologic.i18nservice.audit.service.AuditLogService;
import de.innologic.i18nservice.common.exception.ConflictException;
import de.innologic.i18nservice.common.exception.NotFoundException;
import de.innologic.i18nservice.common.util.ProjectKeyValidator;
import de.innologic.i18nservice.project.dto.CreateProjectRequest;
import de.innologic.i18nservice.project.dto.ProjectScopeResponse;
import de.innologic.i18nservice.project.model.ProjectScopeEntity;
import de.innologic.i18nservice.project.repo.ProjectScopeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;

@Service
public class ProjectScopeService {

    private final ProjectScopeRepository repo;
    private final AuditLogService audit;

    public ProjectScopeService(ProjectScopeRepository repo, AuditLogService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    @Transactional
    public ProjectScopeResponse create(CreateProjectRequest req, String actor, String tenantId) {
        String projectKey = req.projectKey();
        ProjectKeyValidator.validate(projectKey);
        requireTenantBoundProject(projectKey, tenantId);

        if (repo.existsByProjectKey(projectKey)) {
            throw new ConflictException("Project already exists: " + projectKey);
        }

        ProjectScopeEntity e = new ProjectScopeEntity();
        e.setProjectKey(projectKey);
        e.setDisplayName(normalizeNullable(req.displayName()));
        // default/fallback bleiben null und werden über language-settings gesetzt

        ProjectScopeEntity saved = repo.save(e);

        auditSafe(projectKey, actor,
                "PROJECT_CREATE",
                "PROJECT",
                projectKey,
                Map.of("displayName", saved.getDisplayName()),
                null);

        return ProjectScopeResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectScopeResponse> list(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new AccessDeniedException("Missing tenant_id in JWT");
        }
        return repo.findByProjectKeyOrderByProjectKeyAsc(tenantId).stream()
                .map(ProjectScopeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectScopeResponse get(String projectKey) {
        ProjectKeyValidator.validate(projectKey);

        ProjectScopeEntity e = repo.findByProjectKey(projectKey)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectKey));

        return ProjectScopeResponse.from(e);
    }

    private String normalizeNullable(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }

    private void requireTenantBoundProject(String projectKey, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new AccessDeniedException("Missing tenant_id in JWT");
        }
        if (!tenantId.equals(projectKey)) {
            throw new AccessDeniedException("projectKey must match tenant_id");
        }
    }

    private void auditSafe(String projectKey,
                           String actor,
                           String action,
                           String entityType,
                           String entityKey,
                           Object details,
                           String requestId) {
        try {
            audit.log(projectKey, actor, action, entityType, entityKey, details, requestId);
        } catch (Exception ignored) {
        }
    }
}

