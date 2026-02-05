package de.innologic.i18nservice.language.service;

import de.innologic.i18nservice.common.exception.ConflictException;
import de.innologic.i18nservice.common.exception.NotFoundException;
import de.innologic.i18nservice.language.dto.CreateLanguageRequest;
import de.innologic.i18nservice.language.dto.LanguageResponse;
import de.innologic.i18nservice.language.dto.UpdateLanguageRequest;
import de.innologic.i18nservice.language.model.LanguageEntity;
import de.innologic.i18nservice.language.repo.LanguageRepository;
import de.innologic.i18nservice.projectscope.model.ProjectScopeEntity;
import de.innologic.i18nservice.projectscope.repo.ProjectScopeRepository;
import de.innologic.i18nservice.projectscope.service.ProjectKeyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LanguageService {

    private final ProjectScopeRepository projectScopeRepository;
    private final LanguageRepository languageRepository;

    /**
     * P0-2 Create
     * Step 6B: Wenn noch kein Default gesetzt ist, wird die erste Sprache Default.
     */
    @Transactional
    public LanguageResponse create(String projectKey, CreateLanguageRequest req, String actor) {
        ProjectKeyValidator.validate(projectKey);

        ProjectScopeEntity scope = projectScopeRepository.findByProjectKey(projectKey)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectKey));

        String code = normalizeLanguageCode(req.languageCode());

        if (languageRepository.existsByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)) {
            throw new ConflictException("Language already exists: " + code);
        }

        LanguageEntity e = new LanguageEntity();
        e.setProjectKey(projectKey);
        e.setLanguageCode(code);
        e.setName(req.name());
        e.setActive(true);
        e.setDeleted(false);
        e.setCreatedBy(actor);
        e.setModifiedBy(actor);

        LanguageEntity saved = languageRepository.save(e);

        // Step 6B: Default automatisch setzen, wenn noch nicht vorhanden
        if (isBlank(scope.getDefaultLanguageCode())) {
            scope.setDefaultLanguageCode(saved.getLanguageCode());
            projectScopeRepository.save(scope);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LanguageResponse> list(String projectKey) {
        ProjectKeyValidator.validate(projectKey);
        ensureProjectExists(projectKey);

        return languageRepository.findByProjectKeyAndDeletedFalseOrderByLanguageCodeAsc(projectKey)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LanguageResponse get(String projectKey, String languageCode) {
        ProjectKeyValidator.validate(projectKey);
        ensureProjectExists(projectKey);

        String code = normalizeLanguageCode(languageCode);

        LanguageEntity e = languageRepository.findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)
                .orElseThrow(() -> new NotFoundException("Language not found: " + code));

        return toResponse(e);
    }

    /**
     * P0-2 Update
     * Step 6C: Default-Sprache darf nicht deaktiviert werden.
     */
    @Transactional
    public LanguageResponse update(String projectKey, String languageCode, UpdateLanguageRequest req, String actor) {
        ProjectKeyValidator.validate(projectKey);

        ProjectScopeEntity scope = projectScopeRepository.findByProjectKey(projectKey)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectKey));

        String code = normalizeLanguageCode(languageCode);

        LanguageEntity e = languageRepository.findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)
                .orElseThrow(() -> new NotFoundException("Language not found: " + code));

        // zusätzlicher Guard (optional) – @Version macht's sowieso, aber so gibt's klare Fehlermeldung
        if (e.getVersion() != req.expectedVersion()) {
            throw new ConflictException("Version conflict. Current=" + e.getVersion() + ", expected=" + req.expectedVersion());
        }

        // Step 6C: Default darf nicht deaktiviert werden
        if (!req.active() && code.equals(scope.getDefaultLanguageCode())) {
            throw new ConflictException("Cannot deactivate default language: " + code);
        }

        e.setName(req.name());
        e.setActive(req.active());
        e.setModifiedBy(actor);

        LanguageEntity saved = languageRepository.save(e);
        return toResponse(saved);
    }

    /**
     * P0-2 Soft Delete
     * Step 6A: Default/Fallback darf nicht gelöscht werden.
     */
    @Transactional
    public void softDelete(String projectKey, String languageCode, String actor) {
        ProjectKeyValidator.validate(projectKey);

        ProjectScopeEntity scope = projectScopeRepository.findByProjectKey(projectKey)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectKey));

        String code = normalizeLanguageCode(languageCode);

        // Step 6A: Default/Fallback darf nicht gelöscht werden
        if (code.equals(scope.getDefaultLanguageCode())) {
            throw new ConflictException("Cannot delete default language: " + code);
        }
        if (!isBlank(scope.getFallbackLanguageCode()) && code.equals(scope.getFallbackLanguageCode())) {
            throw new ConflictException("Cannot delete fallback language: " + code);
        }

        LanguageEntity e = languageRepository.findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)
                .orElseThrow(() -> new NotFoundException("Language not found: " + code));

        e.setDeleted(true);
        e.setDeletedAt(Instant.now());
        e.setDeletedBy(actor);
        e.setModifiedBy(actor);

        languageRepository.save(e);
    }

    /**
     * P0-2 Restore
     */
    @Transactional
    public LanguageResponse restore(String projectKey, String languageCode, String actor) {
        ProjectKeyValidator.validate(projectKey);
        ensureProjectExists(projectKey);

        String code = normalizeLanguageCode(languageCode);

        if (languageRepository.existsByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)) {
            throw new ConflictException("Cannot restore. Active language with same code exists: " + code);
        }

        LanguageEntity e = languageRepository.findByProjectKeyAndLanguageCodeAndDeletedTrue(projectKey, code)
                .orElseThrow(() -> new NotFoundException("Deleted language not found: " + code));

        e.setDeleted(false);
        e.setDeletedAt(null);
        e.setDeletedBy(null);
        e.setModifiedBy(actor);

        LanguageEntity saved = languageRepository.save(e);
        return toResponse(saved);
    }

    // -------------------------
    // Helpers
    // -------------------------

    private void ensureProjectExists(String projectKey) {
        if (!projectScopeRepository.existsByProjectKey(projectKey)) {
            throw new NotFoundException("Project not found: " + projectKey);
        }
    }

    private String normalizeLanguageCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("languageCode is required");
        }
        String trimmed = code.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("languageCode is required");
        }
        return trimmed;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private LanguageResponse toResponse(LanguageEntity e) {
        // Passe den Konstruktor an, falls dein LanguageResponse andere Felder hat.
        return new LanguageResponse(
                e.getId(),
                e.getProjectKey(),
                e.getLanguageCode(),
                e.getName(),
                e.isActive(),
                e.isDeleted(),
                e.getVersion()
        );
    }
}
