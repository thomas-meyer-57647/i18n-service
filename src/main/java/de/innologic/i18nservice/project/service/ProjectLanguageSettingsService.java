package de.innologic.i18nservice.project.service;

import de.innologic.i18nservice.audit.service.AuditLogService;
import de.innologic.i18nservice.common.exception.ConflictException;
import de.innologic.i18nservice.common.exception.NotFoundException;
import de.innologic.i18nservice.common.util.LanguageCodeNormalizer;
import de.innologic.i18nservice.common.util.ProjectKeyValidator;
import de.innologic.i18nservice.language.model.LanguageEntity;
import de.innologic.i18nservice.language.repo.LanguageRepository;
import de.innologic.i18nservice.project.dto.LanguageSettingsResponse;
import de.innologic.i18nservice.project.dto.UpdateLanguageSettingsRequest;
import de.innologic.i18nservice.project.model.ProjectScopeEntity;
import de.innologic.i18nservice.project.repo.ProjectScopeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ProjectLanguageSettingsService {

    private final ProjectScopeRepository scopeRepo;
    private final LanguageRepository languageRepo;
    private final AuditLogService audit;

    public ProjectLanguageSettingsService(ProjectScopeRepository scopeRepo,
                                          LanguageRepository languageRepo,
                                          AuditLogService audit) {
        this.scopeRepo = scopeRepo;
        this.languageRepo = languageRepo;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public LanguageSettingsResponse get(String projectKey) {
        ProjectKeyValidator.validate(projectKey);

        ProjectScopeEntity scope = scopeRepo.findByProjectKey(projectKey)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectKey));

        return new LanguageSettingsResponse(
                scope.getProjectKey(),
                scope.getDefaultLanguageCode(),
                scope.getFallbackLanguageCode(),
                scope.getVersion()
        );
    }

    @Transactional
    public LanguageSettingsResponse update(String projectKey, UpdateLanguageSettingsRequest req, String actor) {
        ProjectKeyValidator.validate(projectKey);

        ProjectScopeEntity scope = scopeRepo.findByProjectKey(projectKey)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectKey));

        if (scope.getVersion() != req.expectedVersion()) {
            throw new ConflictException("Version conflict. Current=" + scope.getVersion() + ", expected=" + req.expectedVersion());
        }

        String def = normalizeRequired(req.defaultLanguageCode(), "defaultLanguageCode");
        String fb  = normalizeNullable(req.fallbackLanguageCode(), "fallbackLanguageCode");

        ensureActiveLanguageExists(projectKey, def);
        if (fb != null) {
            ensureActiveLanguageExists(projectKey, fb);
        }

        scope.setDefaultLanguageCode(def);
        scope.setFallbackLanguageCode(fb);

        ProjectScopeEntity saved = scopeRepo.save(scope);

        auditSafe(projectKey, actor,
                "PROJECT_SETTINGS_UPDATE",
                "PROJECT_SETTINGS",
                projectKey,
                Map.of("default", def, "fallback", fb),
                null);

        return new LanguageSettingsResponse(saved.getProjectKey(), def, fb, saved.getVersion());
    }

    private void ensureActiveLanguageExists(String projectKey, String languageCode) {
        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, languageCode)
                .orElseThrow(() -> new NotFoundException("Language not found (or deleted): " + languageCode));

        if (!lang.isActive()) {
            throw new ConflictException("Language is inactive: " + languageCode);
        }
    }

    private String normalizeRequired(String raw, String fieldName) {
        String n = LanguageCodeNormalizer.normalize(raw);
        if (n == null || !LanguageCodeNormalizer.isPlausible(n)) {
            throw new IllegalArgumentException("Invalid " + fieldName + ". Use e.g. de, de-DE, zh-Hant-TW");
        }
        return n;
    }

    private String normalizeNullable(String raw, String fieldName) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String n = LanguageCodeNormalizer.normalize(raw);
        if (n == null || !LanguageCodeNormalizer.isPlausible(n)) {
            throw new IllegalArgumentException("Invalid " + fieldName + ". Use e.g. de, de-DE, zh-Hant-TW");
        }
        return n;
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
