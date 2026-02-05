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

        ProjectScopeEntity scope = getScope(projectKey);

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
        getScope(projectKey); // ensures project exists

        return languageRepository.findByProjectKeyAndDeletedFalseOrderByLanguageCodeAsc(projectKey)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LanguageResponse get(String projectKey, String languageCode) {
        ProjectKeyValidator.validate(projectKey);
        getScope(projectKey); // ensures project exists

        String code = normalizeLanguageCode(languageCode);

        LanguageEntity e = languageRepository.findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)
                .orElseThrow(() -> new NotFoundException("Language not found: " + code));

        return toResponse(e);
    }

    /**
     * P0-2 Update
     * P0-4 Guardrails: Default/Fallback dürfen nicht deaktiviert werden.
     */
    @Transactional
    public LanguageResponse update(String projectKey, String languageCode, UpdateLanguageRequest req, String actor) {
        ProjectKeyValidator.validate(projectKey);

        ProjectScopeEntity scope = getScope(projectKey);

        String code = normalizeLanguageCode(languageCode);

        LanguageEntity e = languageRepository.findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)
                .orElseThrow(() -> new NotFoundException("Language not found: " + code));

        // expliziter Version-Check für klare Fehlermeldung
        if (e.getVersion() != req.expectedVersion()) {
            throw new ConflictException("Version conflict. Current=" + e.getVersion() + ", expected=" + req.expectedVersion());
        }

        // P0-4: Default/Fallback dürfen nicht deaktiviert werden
        if (!req.active()) {
            ensureNotDefaultOrFallback(scope, code, "deactivate");
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

        ProjectScopeEntity scope = getScope(projectKey);
        String code = normalizeLanguageCode(languageCode);

        ensureNotDefaultOrFallback(scope, code, "delete");

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
        getScope(projectKey); // ensures project exists

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

    private ProjectScopeEntity getScope(String projectKey) {
        return projectScopeRepository.findByProjectKey(projectKey)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectKey));
    }

    private void ensureNotDefaultOrFallback(ProjectScopeEntity scope, String code, String action) {
        if (code.equals(scope.getDefaultLanguageCode())) {
            throw new ConflictException("Cannot " + action + " default language: " + code);
        }
        if (!isBlank(scope.getFallbackLanguageCode()) && code.equals(scope.getFallbackLanguageCode())) {
            throw new ConflictException("Cannot " + action + " fallback language: " + code);
        }
    }

    /**
     * P0-4: Kanonische Normalisierung
     * - de-de -> de-DE
     * - zh-hant-tw -> zh-Hant-TW
     *
     * (kein vollständiger BCP-47 Validator, aber robust genug für P0)
     */
    private String normalizeLanguageCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("languageCode is required");
        }
        String[] parts = code.trim().split("-");
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;

            if (i == 0) {
                parts[i] = p.toLowerCase(); // language
            } else if (p.length() == 4) {
                // script: Title Case (Hant)
                parts[i] = p.substring(0, 1).toUpperCase() + p.substring(1).toLowerCase();
            } else if (p.length() == 2 || p.length() == 3) {
                // region (DE, TW) oder numeric (419)
                parts[i] = p.toUpperCase();
            } else {
                // variants etc.: wie geliefert (oder lower, wenn du willst)
                parts[i] = p;
            }
        }
        return String.join("-", parts);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private LanguageResponse toResponse(LanguageEntity e) {
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

