package de.innologic.i18nservice.language.service;

import de.innologic.i18nservice.common.exception.ConflictException;
import de.innologic.i18nservice.common.exception.NotFoundException;
import de.innologic.i18nservice.language.dto.CreateLanguageRequest;
import de.innologic.i18nservice.language.dto.LanguageResponse;
import de.innologic.i18nservice.language.dto.UpdateLanguageRequest;
import de.innologic.i18nservice.language.model.LanguageEntity;
import de.innologic.i18nservice.language.repo.LanguageRepository;
import de.innologic.i18nservice.projectscope.repo.ProjectScopeRepository;
import de.innologic.i18nservice.projectscope.service.ProjectKeyValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class LanguageService {

    private final ProjectScopeRepository scopeRepo;
    private final LanguageRepository languageRepo;

    public LanguageService(ProjectScopeRepository scopeRepo, LanguageRepository languageRepo) {
        this.scopeRepo = scopeRepo;
        this.languageRepo = languageRepo;
    }

    @Transactional
    public LanguageResponse create(String projectKey, CreateLanguageRequest req, String actor) {
        ensureProjectExists(projectKey);

        String code = normalizeCode(req.languageCode());

        if (languageRepo.existsByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)) {
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

        return LanguageResponse.from(languageRepo.save(e));
    }

    @Transactional(readOnly = true)
    public List<LanguageResponse> list(String projectKey) {
        ensureProjectExists(projectKey);

        return languageRepo.findByProjectKeyAndDeletedFalseOrderByLanguageCodeAsc(projectKey)
                .stream()
                .map(LanguageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LanguageResponse get(String projectKey, String languageCode) {
        ensureProjectExists(projectKey);

        LanguageEntity e = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, normalizeCode(languageCode))
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCode));

        return LanguageResponse.from(e);
    }

    @Transactional
    public LanguageResponse update(String projectKey, String languageCode, UpdateLanguageRequest req, String actor) {
        ensureProjectExists(projectKey);

        LanguageEntity e = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, normalizeCode(languageCode))
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCode));

        if (e.getVersion() != req.expectedVersion()) {
            throw new ConflictException(
                    "Version conflict. Current=" + e.getVersion() + ", expected=" + req.expectedVersion()
            );
        }

        e.setName(req.name());
        e.setActive(req.active());
        e.setModifiedBy(actor);

        return LanguageResponse.from(languageRepo.save(e));
    }

    @Transactional
    public void softDelete(String projectKey, String languageCode, String actor) {
        ensureProjectExists(projectKey);

        LanguageEntity e = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, normalizeCode(languageCode))
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCode));

        e.setDeleted(true);
        e.setDeletedAt(Instant.now());
        e.setDeletedBy(actor);
        e.setModifiedBy(actor);

        languageRepo.save(e);
    }

    @Transactional
    public LanguageResponse restore(String projectKey, String languageCode, String actor) {
        ensureProjectExists(projectKey);

        String code = normalizeCode(languageCode);

        if (languageRepo.existsByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)) {
            throw new ConflictException("Cannot restore. Active language with same code exists: " + code);
        }

        LanguageEntity e = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedTrue(projectKey, code)
                .orElseThrow(() -> new NotFoundException("Deleted language not found: " + languageCode));

        e.setDeleted(false);
        e.setDeletedAt(null);
        e.setDeletedBy(null);
        e.setModifiedBy(actor);

        return LanguageResponse.from(languageRepo.save(e));
    }

    private void ensureProjectExists(String projectKey) {
        ProjectKeyValidator.validate(projectKey);

        if (!scopeRepo.existsByProjectKey(projectKey)) {
            throw new NotFoundException("Project not found: " + projectKey);
        }
    }

    private String normalizeCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("languageCode must not be null");
        }
        String trimmed = code.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("languageCode must not be blank");
        }
        return trimmed;
    }
}
