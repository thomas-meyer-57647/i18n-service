package de.innologic.i18nservice.settings.service;
import de.innologic.i18nservice.common.exception.ConflictException;
import de.innologic.i18nservice.common.exception.NotFoundException;
import de.innologic.i18nservice.language.model.LanguageEntity;
import de.innologic.i18nservice.language.repo.LanguageRepository;
import de.innologic.i18nservice.projectscope.model.ProjectScopeEntity;
import de.innologic.i18nservice.projectscope.repo.ProjectScopeRepository;
import de.innologic.i18nservice.projectscope.service.ProjectKeyValidator;
import de.innologic.i18nservice.settings.dto.LanguageSettingsResponse;
import de.innologic.i18nservice.settings.dto.UpdateLanguageSettingsRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectLanguageSettingsService {

    private final ProjectScopeRepository scopeRepo;
    private final LanguageRepository languageRepo;

    public ProjectLanguageSettingsService(ProjectScopeRepository scopeRepo, LanguageRepository languageRepo) {
        this.scopeRepo = scopeRepo;
        this.languageRepo = languageRepo;
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

        String def = normalize(req.defaultLanguageCode());
        String fb  = normalize(req.fallbackLanguageCode());

        if (def == null || def.isBlank()) {
            throw new IllegalArgumentException("defaultLanguageCode is required");
        }

        // Default/Fallback müssen existieren und dürfen nicht deleted sein
        ensureActiveLanguageExists(projectKey, def);

        if (fb != null && !fb.isBlank()) {
            ensureActiveLanguageExists(projectKey, fb);
        } else {
            fb = null;
        }

        scope.setDefaultLanguageCode(def);
        scope.setFallbackLanguageCode(fb);

        scopeRepo.save(scope);

        return new LanguageSettingsResponse(scope.getProjectKey(), def, fb, scope.getVersion());
    }

    private void ensureActiveLanguageExists(String projectKey, String languageCode) {
        LanguageEntity lang = languageRepo.findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, languageCode)
                .orElseThrow(() -> new NotFoundException("Language not found (or deleted): " + languageCode));

        if (!lang.isActive()) {
            throw new ConflictException("Language is inactive: " + languageCode);
        }
    }

    private String normalize(String code) {
        return code == null ? null : code.trim();
    }
}
