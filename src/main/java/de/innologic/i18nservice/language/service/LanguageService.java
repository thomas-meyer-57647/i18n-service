package de.innologic.i18nservice.language.service;

import de.innologic.i18nservice.audit.service.AuditLogService;
import de.innologic.i18nservice.bundle.cache.BundleCache;
import de.innologic.i18nservice.bundle.model.LanguageBundleVersionEntity;
import de.innologic.i18nservice.bundle.repo.LanguageBundleRepository;
import de.innologic.i18nservice.bundle.repo.LanguageBundleVersionRepository;
import de.innologic.i18nservice.common.exception.ConflictException;
import de.innologic.i18nservice.common.exception.NotFoundException;
import de.innologic.i18nservice.common.util.LanguageCodeNormalizer;
import de.innologic.i18nservice.common.util.ProjectKeyValidator;
import de.innologic.i18nservice.language.dto.CreateLanguageRequest;
import de.innologic.i18nservice.language.dto.LanguageResponse;
import de.innologic.i18nservice.language.dto.UpdateLanguageRequest;
import de.innologic.i18nservice.language.model.LanguageEntity;
import de.innologic.i18nservice.language.repo.LanguageRepository;
import de.innologic.i18nservice.project.model.ProjectScopeEntity;
import de.innologic.i18nservice.project.repo.ProjectScopeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class LanguageService {

    private final ProjectScopeRepository scopeRepo;
    private final LanguageRepository languageRepository;
    private final AuditLogService audit;
    private final LanguageBundleRepository bundleRepo;
    private final LanguageBundleVersionRepository bundleVersionRepo;
    private final BundleCache bundleCache;

    public LanguageService(ProjectScopeRepository scopeRepo,
                           LanguageRepository languageRepository,
                           AuditLogService audit,
                           LanguageBundleRepository bundleRepo,
                           LanguageBundleVersionRepository bundleVersionRepo,
                           BundleCache bundleCache) {
        this.scopeRepo = scopeRepo;
        this.languageRepository = languageRepository;
        this.audit = audit;
        this.bundleRepo = bundleRepo;
        this.bundleVersionRepo = bundleVersionRepo;
        this.bundleCache = bundleCache;
    }

    // -------------------------------------------------
    // Create / Read
    // -------------------------------------------------

    @Transactional
    public LanguageResponse create(String projectKey, CreateLanguageRequest req, String actor) {
        ProjectScopeEntity scope = loadScope(projectKey);

        String code = normalizeLanguageCode(req.languageCode());

        // aktives (nicht deleted) Duplikat verhindern
        if (languageRepository.existsByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)) {
            throw new ConflictException("Language already exists: " + code);
        }

        // Edge-Case Fix (UNIQUE(project, code, is_deleted)):
        // Wenn deleted-Version existiert -> nicht neu anlegen, sonst später SoftDelete-Konflikt.
        if (languageRepository.existsByProjectKeyAndLanguageCodeAndDeletedTrue(projectKey, code)) {
            throw new ConflictException("Language exists in trash. Restore or purge first: " + code);
        }

        LanguageEntity e = new LanguageEntity();
        e.setProjectKey(projectKey);
        e.setLanguageCode(code);
        e.setName(req.name());
        e.setActive(true);

        e.setDeleted(false);
        e.setDeletedAt(null);
        e.setDeletedBy(null);

        e.setCreatedBy(actor);
        e.setModifiedBy(actor);

        LanguageEntity saved = languageRepository.save(e);

        auditSafe(projectKey, actor,
                "LANGUAGE_CREATE",
                "LANGUAGE",
                code,
                Map.of("name", saved.getName(), "active", saved.isActive()),
                null);

        // Optional, aber praktisch: wenn kein Default gesetzt ist, setze ihn auf die erste Sprache
        if (scope.getDefaultLanguageCode() == null || scope.getDefaultLanguageCode().isBlank()) {
            scope.setDefaultLanguageCode(code);
            scopeRepo.save(scope);

            auditSafe(projectKey, actor,
                    "PROJECT_SETTINGS_AUTO_DEFAULT",
                    "PROJECT_SETTINGS",
                    projectKey,
                    Map.of("default", code),
                    null);
        }

        return LanguageResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<LanguageResponse> listActive(String projectKey) {
        loadScope(projectKey);

        return languageRepository
                .findByProjectKeyAndDeletedFalseAndActiveTrueOrderByLanguageCodeAsc(projectKey)
                .stream()
                .map(LanguageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LanguageResponse> listAll(String projectKey) {
        loadScope(projectKey);

        return languageRepository.findByProjectKeyOrderByLanguageCodeAsc(projectKey)
                .stream()
                .map(LanguageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LanguageResponse> listDeleted(String projectKey) {
        loadScope(projectKey);

        return languageRepository.findByProjectKeyAndDeletedTrueOrderByLanguageCodeAsc(projectKey)
                .stream()
                .map(LanguageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LanguageResponse get(String projectKey, String languageCodeRaw) {
        loadScope(projectKey);

        String code = normalizeLanguageCode(languageCodeRaw);

        LanguageEntity e = languageRepository
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)
                .orElseThrow(() -> new NotFoundException("Language not found: " + code));

        return LanguageResponse.from(e);
    }

    // -------------------------------------------------
    // Update / Delete / Restore / Purge
    // -------------------------------------------------

    @Transactional
    public LanguageResponse update(String projectKey, String languageCodeRaw, UpdateLanguageRequest req, String actor) {
        ProjectScopeEntity scope = loadScope(projectKey);

        String code = normalizeLanguageCode(languageCodeRaw);

        LanguageEntity e = languageRepository
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)
                .orElseThrow(() -> new NotFoundException("Language not found: " + code));

        if (e.getVersion() != req.expectedVersion()) {
            throw new ConflictException("Version conflict. Current=" + e.getVersion() + ", expected=" + req.expectedVersion());
        }

        // Default/Fallback darf nicht deaktiviert werden
        if (!req.active()) {
            ensureNotDefaultOrFallback(scope, code, "deactivate");
        }

        e.setName(req.name());
        e.setActive(req.active());
        e.setModifiedBy(actor);

        LanguageEntity saved = languageRepository.save(e);

        auditSafe(projectKey, actor,
                "LANGUAGE_UPDATE",
                "LANGUAGE",
                code,
                Map.of("name", saved.getName(), "active", saved.isActive(), "version", saved.getVersion()),
                null);

        return LanguageResponse.from(saved);
    }

    @Transactional
    public void softDelete(String projectKey, String languageCodeRaw, String actor) {
        ProjectScopeEntity scope = loadScope(projectKey);

        String code = normalizeLanguageCode(languageCodeRaw);

        LanguageEntity e = languageRepository
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)
                .orElseThrow(() -> new NotFoundException("Language not found: " + code));

        ensureNotDefaultOrFallback(scope, code, "delete");

        // Edge-Case Fix: wenn schon deleted-Version existiert -> kein SoftDelete möglich
        if (languageRepository.existsByProjectKeyAndLanguageCodeAndDeletedTrue(projectKey, code)) {
            throw new ConflictException("Cannot soft-delete. A deleted version already exists. Purge or restore first: " + code);
        }

        e.setDeleted(true);
        e.setDeletedAt(Instant.now());
        e.setDeletedBy(actor);
        e.setModifiedBy(actor);

        languageRepository.save(e);

        auditSafe(projectKey, actor,
                "LANGUAGE_SOFT_DELETE",
                "LANGUAGE",
                code,
                Map.of("deletedAt", e.getDeletedAt()),
                null);
    }

    @Transactional
    public LanguageResponse restore(String projectKey, String languageCodeRaw, String actor) {
        loadScope(projectKey);

        String code = normalizeLanguageCode(languageCodeRaw);

        // Restore nur wenn keine aktive Version existiert
        if (languageRepository.existsByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)) {
            throw new ConflictException("Cannot restore. Active language with same code exists: " + code);
        }

        LanguageEntity e = languageRepository
                .findByProjectKeyAndLanguageCodeAndDeletedTrue(projectKey, code)
                .orElseThrow(() -> new NotFoundException("Deleted language not found: " + code));

        e.setDeleted(false);
        e.setDeletedAt(null);
        e.setDeletedBy(null);
        e.setModifiedBy(actor);

        LanguageEntity saved = languageRepository.save(e);

        auditSafe(projectKey, actor,
                "LANGUAGE_RESTORE",
                "LANGUAGE",
                code,
                null,
                null);

        return LanguageResponse.from(saved);
    }

    @Transactional
    public void purge(String projectKey, String languageCodeRaw, boolean force, String actor) {
        ProjectScopeEntity scope = loadScope(projectKey);
        String code = normalizeLanguageCode(languageCodeRaw);

        // Ziel ermitteln:
        // - Standard: nur deleted=true purgebar
        // - force: falls nicht deleted, dann deleted=false purgebar
        LanguageEntity target = languageRepository
                .findByProjectKeyAndLanguageCodeAndDeletedTrue(projectKey, code)
                .orElseGet(() -> {
                    if (!force) {
                        throw new ConflictException("Language is not in trash. Soft-delete first: " + code);
                    }
                    return languageRepository
                            .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)
                            .orElseThrow(() -> new NotFoundException("Language not found: " + code));
                });

        // Guardrail: Default/Fallback darf nie purged werden
        ensureNotDefaultOrFallback(scope, code, "purge");

        // 1) Bundle + Datei löschen (falls vorhanden) -> wegen FK RESTRICT muss das vor Language-Delete passieren
        boolean bundleRemoved = deleteBundleIfExists(projectKey, code, target.getId(), actor);

        // 2) Cache invalidieren (sicher, unabhängig davon ob Bundle existierte)
        bundleCache.invalidate(projectKey, code);

        // 3) Language Row hard löschen
        languageRepository.delete(target);

        // 4) Audit
        auditSafe(projectKey, actor,
                "LANGUAGE_PURGE",
                "LANGUAGE",
                code,
                Map.of(
                        "force", force,
                        "wasDeleted", target.isDeleted(),
                        "bundleRemoved", bundleRemoved
                ),
                null);
    }

    // -------------------------------------------------
    // Helpers
    // -------------------------------------------------

    private ProjectScopeEntity loadScope(String projectKey) {
        ProjectKeyValidator.validate(projectKey);
        return scopeRepo.findByProjectKey(projectKey)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectKey));
    }

    private String normalizeLanguageCode(String raw) {
        String n = LanguageCodeNormalizer.normalize(raw);
        if (n == null || !LanguageCodeNormalizer.isPlausible(n)) {
            throw new IllegalArgumentException("Invalid languageCode. Use e.g. de, de-DE, zh-Hant-TW");
        }
        return n;
    }

    private void ensureNotDefaultOrFallback(ProjectScopeEntity scope, String code, String action) {
        String def = LanguageCodeNormalizer.normalize(scope.getDefaultLanguageCode());
        String fb = LanguageCodeNormalizer.normalize(scope.getFallbackLanguageCode());

        if (def != null && code.equals(def)) {
            throw new ConflictException("Cannot " + action + " default language: " + code);
        }
        if (fb != null && code.equals(fb)) {
            throw new ConflictException("Cannot " + action + " fallback language: " + code);
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
            // Audit darf fachliche Operationen nicht kaputt machen
        }
    }

    private boolean deleteBundleIfExists(String projectKey, String languageCode, Long languageId, String actor) {
        var currentOpt = bundleRepo.findByLanguageId(languageId);
        List<LanguageBundleVersionEntity> versions = bundleVersionRepo.findByLanguageIdOrderByBundleVersionDesc(languageId);

        if (currentOpt.isEmpty() && versions.isEmpty()) {
            return false;
        }

        // alle eindeutigen Paths sammeln
        java.util.Set<String> paths = new java.util.LinkedHashSet<>();
        currentOpt.ifPresent(b -> {
            if (b.getStoragePath() != null && !b.getStoragePath().isBlank()) paths.add(b.getStoragePath());
        });
        for (LanguageBundleVersionEntity v : versions) {
            if (v.getStoragePath() != null && !v.getStoragePath().isBlank()) paths.add(v.getStoragePath());
        }

        // DB weg
        currentOpt.ifPresent(bundleRepo::delete);
        if (!versions.isEmpty()) {
            bundleVersionRepo.deleteAll(versions);
        }

        // Files weg
        for (String p : paths) {
            deleteFileIfExists(p);
        }

        // Cache sicherheitshalber invalidieren
        bundleCache.invalidate(projectKey, languageCode);

        auditSafe(projectKey, actor,
                "BUNDLE_PURGE_WITH_LANGUAGE",
                "BUNDLE",
                languageCode,
                Map.of(
                        "versionsDeleted", versions.size(),
                        "files", paths.size()
                ),
                null);

        return true;
    }

    private void deleteFileIfExists(String path) {
        if (path == null || path.isBlank()) return;
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (Exception ignored) {
        }
    }
}

