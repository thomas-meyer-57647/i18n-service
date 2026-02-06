package de.innologic.i18nservice.bundle.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.i18nservice.audit.service.AuditLogService;
import de.innologic.i18nservice.bundle.cache.BundleCache;
import de.innologic.i18nservice.bundle.dto.BundleDiffResponse;
import de.innologic.i18nservice.bundle.dto.BundleMetaResponse;
import de.innologic.i18nservice.bundle.dto.BundleVersionResponse;
import de.innologic.i18nservice.bundle.model.LanguageBundleEntity;
import de.innologic.i18nservice.bundle.model.LanguageBundleVersionEntity;
import de.innologic.i18nservice.bundle.repo.LanguageBundleRepository;
import de.innologic.i18nservice.bundle.repo.LanguageBundleVersionRepository;
import de.innologic.i18nservice.common.exception.ConflictException;
import de.innologic.i18nservice.common.exception.NotFoundException;
import de.innologic.i18nservice.common.util.LanguageCodeNormalizer;
import de.innologic.i18nservice.common.util.ProjectKeyValidator;
import de.innologic.i18nservice.language.model.LanguageEntity;
import de.innologic.i18nservice.language.repo.LanguageRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class LanguageBundleService {

    // P0: flache JSON Map -> Keys validieren
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$");

    private final LanguageRepository languageRepo;
    private final LanguageBundleRepository bundleRepo;
    private final LanguageBundleVersionRepository bundleVersionRepo;
    private final BundleCache bundleCache;
    private final ObjectMapper objectMapper;
    private final AuditLogService audit;
    private final Path basePath;

    public LanguageBundleService(LanguageRepository languageRepo,
                                 LanguageBundleRepository bundleRepo,
                                 LanguageBundleVersionRepository bundleVersionRepo,
                                 BundleCache bundleCache,
                                 ObjectMapper objectMapper,
                                 AuditLogService audit,
                                 @Value("${app.bundle-storage.base-path:./data/i18n}") String basePath) {
        this.languageRepo = languageRepo;
        this.bundleRepo = bundleRepo;
        this.bundleVersionRepo = bundleVersionRepo;
        this.bundleCache = bundleCache;
        this.objectMapper = objectMapper;
        this.audit = audit;
        this.basePath = Paths.get(basePath);
    }

    /**
     * Upload (versioniert):
     * 1) neue Datei schreiben
     * 2) neue Version speichern (Versionshistorie)
     * 3) "aktuelle" DB-Row aktualisieren (zeigt auf die aktive Version)
     * 4) bei Fehler: neue Datei entfernen
     * 5) Cache invalidieren
     */
    @Transactional
    public BundleMetaResponse upload(String projectKey, String languageCodeRaw, MultipartFile file, String actor) {
        ProjectKeyValidator.validate(projectKey);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        String languageCode = normalizeLanguageCode(languageCodeRaw);

        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, languageCode)
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCodeRaw));

        String originalName = (file.getOriginalFilename() == null) ? "bundle.json" : file.getOriginalFilename();
        if (!originalName.toLowerCase(Locale.ROOT).endsWith(".json")) {
            throw new IllegalArgumentException("Only .json bundles are supported in P0");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read uploaded file");
        }

        validateJsonBundle(bytes);

        String sha = sha256Hex(bytes);

        Path newPath = null;

        try {
            Path dir = basePath.resolve(projectKey).resolve(languageCode);
            Files.createDirectories(dir);

            // nächste Bundle-Version bestimmen (1..n)
            int nextVersion = bundleVersionRepo.findTopByLanguageIdOrderByBundleVersionDesc(lang.getId())
                    .map(v -> v.getBundleVersion() + 1)
                    .orElse(1);

            String safeName = "bundle-v" + nextVersion + "-" + Instant.now().toEpochMilli() + "-" + sha + ".json";
            newPath = dir.resolve(safeName);

            // 1) Datei schreiben (kein Überschreiben)
            Files.write(newPath, bytes, StandardOpenOption.CREATE_NEW);

            Instant now = Instant.now();

            // 2) neue Version persistieren
            LanguageBundleVersionEntity versionEntity = new LanguageBundleVersionEntity();
            versionEntity.setLanguageId(lang.getId());
            versionEntity.setBundleVersion(nextVersion);
            versionEntity.setFileFormat("JSON");
            versionEntity.setOriginalFileName(originalName);
            versionEntity.setContentType(file.getContentType());
            versionEntity.setStoragePath(newPath.toString());
            versionEntity.setSha256(sha);
            versionEntity.setSizeBytes(bytes.length);
            versionEntity.setUploadedAt(now);
            versionEntity.setUploadedBy(actor);

            LanguageBundleVersionEntity savedVersion = bundleVersionRepo.save(versionEntity);

            // 3) aktuelle DB-Row updaten (zeigt immer auf die aktive Version)
            LanguageBundleEntity current = bundleRepo.findByLanguageId(lang.getId())
                    .orElseGet(() -> {
                        LanguageBundleEntity e = new LanguageBundleEntity();
                        e.setLanguageId(lang.getId());
                        return e;
                    });

            current.setBundleVersion(savedVersion.getBundleVersion());
            current.setFileFormat(savedVersion.getFileFormat());
            current.setOriginalFileName(savedVersion.getOriginalFileName());
            current.setContentType(savedVersion.getContentType());
            current.setStoragePath(savedVersion.getStoragePath());
            current.setSha256(savedVersion.getSha256());
            current.setSizeBytes(savedVersion.getSizeBytes());
            current.setUploadedAt(savedVersion.getUploadedAt());
            current.setUploadedBy(savedVersion.getUploadedBy());

            LanguageBundleEntity savedCurrent = bundleRepo.save(current);

            // 5) Cache invalidieren
            bundleCache.invalidate(projectKey, languageCode);

            auditSafe(projectKey, actor,
                    "BUNDLE_UPLOAD",
                    "BUNDLE",
                    languageCode,
                    Map.of(
                            "bundleVersion", savedCurrent.getBundleVersion(),
                            "sha256", savedCurrent.getSha256(),
                            "sizeBytes", savedCurrent.getSizeBytes(),
                            "file", savedCurrent.getOriginalFileName()
                    ),
                    null);

            return BundleMetaResponse.from(savedCurrent);

        } catch (FileAlreadyExistsException ex) {
            throw new ConflictException("Bundle with same name already exists (retry upload)");
        } catch (DataIntegrityViolationException ex) {
            // 4) Rollback: neue Datei entfernen
            deleteIfExists(newPath);
            throw new ConflictException("Concurrent upload detected (retry upload)");
        } catch (RuntimeException ex) {
            // 4) Rollback: neue Datei entfernen
            deleteIfExists(newPath);
            throw ex;
        } catch (Exception ex) {
            deleteIfExists(newPath);
            throw new IllegalArgumentException("Could not store bundle file");
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<BundleVersionResponse> listVersions(String projectKey, String languageCodeRaw) {
        ProjectKeyValidator.validate(projectKey);

        String languageCode = normalizeLanguageCode(languageCodeRaw);

        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, languageCode)
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCodeRaw));

        int currentVersion = bundleRepo.findByLanguageId(lang.getId())
                .map(LanguageBundleEntity::getBundleVersion)
                .orElse(0);

        return bundleVersionRepo.findByLanguageIdOrderByBundleVersionDesc(lang.getId())
                .stream()
                .map(v -> BundleVersionResponse.from(v, currentVersion))
                .toList();
    }

    @Transactional(readOnly = true)
    public BundleVersionResponse getVersionMeta(String projectKey, String languageCodeRaw, int bundleVersion) {
        ProjectKeyValidator.validate(projectKey);

        if (bundleVersion < 1) {
            throw new IllegalArgumentException("bundleVersion must be >= 1");
        }

        String languageCode = normalizeLanguageCode(languageCodeRaw);

        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, languageCode)
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCodeRaw));

        int currentVersion = bundleRepo.findByLanguageId(lang.getId())
                .map(LanguageBundleEntity::getBundleVersion)
                .orElse(0);

        LanguageBundleVersionEntity versionEntity = bundleVersionRepo
                .findByLanguageIdAndBundleVersion(lang.getId(), bundleVersion)
                .orElseThrow(() -> new NotFoundException("Bundle version not found: v" + bundleVersion));

        return BundleVersionResponse.from(versionEntity, currentVersion);
    }

    @Transactional
    public BundleMetaResponse rollback(String projectKey, String languageCodeRaw, int targetVersion, String actor) {
        ProjectKeyValidator.validate(projectKey);

        if (targetVersion < 1) {
            throw new IllegalArgumentException("targetVersion must be >= 1");
        }

        String languageCode = normalizeLanguageCode(languageCodeRaw);

        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, languageCode)
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCodeRaw));

        LanguageBundleVersionEntity version = bundleVersionRepo
                .findByLanguageIdAndBundleVersion(lang.getId(), targetVersion)
                .orElseThrow(() -> new NotFoundException("Bundle version not found: v" + targetVersion));

        // Datei muss existieren
        Path p = Paths.get(version.getStoragePath());
        if (!Files.exists(p)) {
            throw new NotFoundException("Bundle file missing on storage for v" + targetVersion);
        }

        int previousVersion = bundleRepo.findByLanguageId(lang.getId())
                .map(LanguageBundleEntity::getBundleVersion)
                .orElse(0);

        LanguageBundleEntity current = bundleRepo.findByLanguageId(lang.getId())
                .orElseGet(() -> {
                    LanguageBundleEntity e = new LanguageBundleEntity();
                    e.setLanguageId(lang.getId());
                    return e;
                });

        current.setBundleVersion(version.getBundleVersion());
        current.setFileFormat(version.getFileFormat());
        current.setOriginalFileName(version.getOriginalFileName());
        current.setContentType(version.getContentType());
        current.setStoragePath(version.getStoragePath());
        current.setSha256(version.getSha256());
        current.setSizeBytes(version.getSizeBytes());
        current.setUploadedAt(version.getUploadedAt());
        current.setUploadedBy(version.getUploadedBy());

        LanguageBundleEntity saved = bundleRepo.save(current);

        // Cache invalidieren
        bundleCache.invalidate(projectKey, languageCode);

        auditSafe(projectKey, actor,
                "BUNDLE_ROLLBACK",
                "BUNDLE",
                languageCode,
                Map.of(
                        "fromVersion", previousVersion,
                        "toVersion", targetVersion,
                        "sha256", saved.getSha256(),
                        "file", saved.getOriginalFileName()
                ),
                null);

        return BundleMetaResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public BundleDiffResponse diff(String projectKey, String languageCodeRaw, int fromVersion, int toVersion) {
        ProjectKeyValidator.validate(projectKey);

        if (fromVersion < 1 || toVersion < 1) {
            throw new IllegalArgumentException("fromVersion/toVersion must be >= 1");
        }

        String languageCode = normalizeLanguageCode(languageCodeRaw);

        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, languageCode)
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCodeRaw));

        LanguageBundleVersionEntity from = bundleVersionRepo
                .findByLanguageIdAndBundleVersion(lang.getId(), fromVersion)
                .orElseThrow(() -> new NotFoundException("Bundle version not found: v" + fromVersion));

        LanguageBundleVersionEntity to = bundleVersionRepo
                .findByLanguageIdAndBundleVersion(lang.getId(), toVersion)
                .orElseThrow(() -> new NotFoundException("Bundle version not found: v" + toVersion));

        Map<String, String> fromMap = readBundleMap(Paths.get(from.getStoragePath()), "v" + fromVersion);
        Map<String, String> toMap = readBundleMap(Paths.get(to.getStoragePath()), "v" + toVersion);

        java.util.Set<String> fromKeys = new java.util.HashSet<>(fromMap.keySet());
        java.util.Set<String> toKeys = new java.util.HashSet<>(toMap.keySet());

        java.util.List<String> added = toKeys.stream()
                .filter(k -> !fromKeys.contains(k))
                .sorted()
                .toList();

        java.util.List<String> removed = fromKeys.stream()
                .filter(k -> !toKeys.contains(k))
                .sorted()
                .toList();

        java.util.List<BundleDiffResponse.ChangedEntry> changed = fromKeys.stream()
                .filter(toKeys::contains)
                .filter(k -> {
                    String a = fromMap.get(k);
                    String b = toMap.get(k);
                    return a == null ? b != null : !a.equals(b);
                })
                .sorted()
                .map(k -> new BundleDiffResponse.ChangedEntry(k, fromMap.get(k), toMap.get(k)))
                .toList();

        return new BundleDiffResponse(
                fromVersion,
                toVersion,
                added.size(),
                removed.size(),
                changed.size(),
                added,
                removed,
                changed
        );
    }

    @Transactional(readOnly = true)
    public StoredFile download(String projectKey, String languageCodeRaw) {
        ProjectKeyValidator.validate(projectKey);

        String languageCode = normalizeLanguageCode(languageCodeRaw);

        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, languageCode)
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCodeRaw));

        LanguageBundleEntity bundle = bundleRepo.findByLanguageId(lang.getId())
                .orElseThrow(() -> new NotFoundException("Bundle not found for language: " + languageCodeRaw));

        Path p = Paths.get(bundle.getStoragePath());
        if (!Files.exists(p)) {
            throw new NotFoundException("Bundle file missing on storage for language: " + languageCodeRaw);
        }

        // SHA wird mitgegeben (für ETag im Controller)
        return new StoredFile(p, bundle.getContentType(), bundle.getOriginalFileName(), bundle.getSha256());
    }

    /**
     * Download einer KONKRETEN Version.
     */
    @Transactional(readOnly = true)
    public StoredFile downloadVersion(String projectKey, String languageCodeRaw, int bundleVersion) {
        ProjectKeyValidator.validate(projectKey);

        if (bundleVersion < 1) {
            throw new IllegalArgumentException("bundleVersion must be >= 1");
        }

        String languageCode = normalizeLanguageCode(languageCodeRaw);

        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, languageCode)
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCodeRaw));

        LanguageBundleVersionEntity versionEntity = bundleVersionRepo
                .findByLanguageIdAndBundleVersion(lang.getId(), bundleVersion)
                .orElseThrow(() -> new NotFoundException("Bundle version not found: v" + bundleVersion));

        Path p = Paths.get(versionEntity.getStoragePath());
        if (!Files.exists(p)) {
            throw new NotFoundException("Bundle file missing on storage for v" + bundleVersion);
        }

        return new StoredFile(p, versionEntity.getContentType(), versionEntity.getOriginalFileName(), versionEntity.getSha256());
    }

    @Transactional(readOnly = true)
    public BundleMetaResponse getMeta(String projectKey, String languageCodeRaw) {
        ProjectKeyValidator.validate(projectKey);

        String languageCode = normalizeLanguageCode(languageCodeRaw);

        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, languageCode)
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCodeRaw));

        LanguageBundleEntity bundle = bundleRepo.findByLanguageId(lang.getId())
                .orElseThrow(() -> new NotFoundException("Bundle not found for language: " + languageCodeRaw));

        return BundleMetaResponse.from(bundle);
    }

    /**
     * Bundle löschen/reset:
     * - aktuelle DB Row löschen
     * - Versionshistorie löschen
     * - alle Dateien löschen (best effort)
     * - Cache invalidieren
     * - Audit inkl. File-Status
     */
    @Transactional
    public void deleteBundle(String projectKey, String languageCodeRaw, String actor) {
        ProjectKeyValidator.validate(projectKey);

        String code = normalizeLanguageCode(languageCodeRaw);

        // 1) Sprache finden (nur nicht-deleted)
        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)
                .orElseThrow(() -> new NotFoundException("Language not found: " + code));

        // 2) aktuelle Bundle-Row finden
        LanguageBundleEntity current = bundleRepo.findByLanguageId(lang.getId())
                .orElseThrow(() -> new NotFoundException("Bundle not found for language: " + code));

        int currentVersion = current.getBundleVersion();

        // 3) Versionshistorie laden
        java.util.List<LanguageBundleVersionEntity> versions = bundleVersionRepo
                .findByLanguageIdOrderByBundleVersionDesc(lang.getId());

        // 4) DB löschen (current + versions)
        bundleRepo.delete(current);
        if (!versions.isEmpty()) {
            bundleVersionRepo.deleteAll(versions);
        }

        // 5) Dateien löschen (best effort) - alle eindeutigen Pfade
        java.util.Set<String> paths = new java.util.LinkedHashSet<>();
        if (current.getStoragePath() != null && !current.getStoragePath().isBlank()) {
            paths.add(current.getStoragePath());
        }
        for (LanguageBundleVersionEntity v : versions) {
            if (v.getStoragePath() != null && !v.getStoragePath().isBlank()) {
                paths.add(v.getStoragePath());
            }
        }

        int existed = 0;
        int deleted = 0;
        java.util.List<String> deleteErrors = new java.util.ArrayList<>();
        for (String p : paths) {
            DeleteFileResult r = deleteFileIfExists(p);
            if (r.existed()) existed++;
            if (r.deleted()) deleted++;
            if (r.error() != null) deleteErrors.add(p + ":" + r.error());
        }

        // 6) Cache invalidieren
        bundleCache.invalidate(projectKey, code);

        // 7) Audit
        auditSafe(projectKey, actor,
                "BUNDLE_DELETE",
                "BUNDLE",
                code,
                Map.of(
                        "currentBundleVersion", currentVersion,
                        "versionsDeleted", versions.size(),
                        "files", paths.size(),
                        "filesExisted", existed,
                        "filesDeleted", deleted,
                        "fileDeleteErrors", deleteErrors
                ),
                null);
    }

    // ------------------------
    // Helpers
    // ------------------------

    private String normalizeLanguageCode(String raw) {
        String n = LanguageCodeNormalizer.normalize(raw);
        if (n == null || !LanguageCodeNormalizer.isPlausible(n)) {
            throw new IllegalArgumentException("Invalid languageCode. Use e.g. de, de-DE, zh-Hant-TW");
        }
        return n;
    }

    private void validateJsonBundle(byte[] bytes) {
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            Map<String, String> map = objectMapper.readValue(is, new TypeReference<>() {});
            if (map.isEmpty()) {
                throw new IllegalArgumentException("Bundle JSON must not be empty");
            }

            for (var e : map.entrySet()) {
                String key = e.getKey();
                String val = e.getValue();

                if (key == null || key.isBlank()) {
                    throw new IllegalArgumentException("Bundle contains empty key");
                }
                if (!KEY_PATTERN.matcher(key).matches()) {
                    throw new IllegalArgumentException("Invalid key '" + key + "'. Allowed: " + KEY_PATTERN.pattern());
                }
                if (val == null) {
                    throw new IllegalArgumentException("Bundle contains null value for key: " + key);
                }
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON bundle (must be flat key->string map)");
        }
    }

    private Map<String, String> readBundleMap(Path path, String label) {
        if (path == null) {
            throw new NotFoundException("Bundle file missing on storage for " + label);
        }
        if (!Files.exists(path)) {
            throw new NotFoundException("Bundle file missing on storage for " + label);
        }

        try {
            byte[] bytes = Files.readAllBytes(path);
            return objectMapper.readValue(bytes, new TypeReference<>() {});
        } catch (NotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot read/parse bundle JSON for " + label);
        }
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available");
        }
    }

    /**
     * Für Upload-Rollback/Replace: best effort, ohne Status.
     */
    private void deleteIfExists(String path) {
        if (path == null || path.isBlank()) return;
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (Exception ignored) {
        }
    }

    private void deleteIfExists(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }

    /**
     * Für deleteBundle(): best effort, aber mit Status fürs Audit.
     */
    private DeleteFileResult deleteFileIfExists(String path) {
        if (path == null || path.isBlank()) {
            return new DeleteFileResult(false, false, "no-path");
        }

        try {
            Path p = Paths.get(path);
            boolean existed = Files.exists(p);
            boolean deleted = Files.deleteIfExists(p);
            return new DeleteFileResult(existed, deleted, null);
        } catch (Exception ex) {
            return new DeleteFileResult(true, false, ex.getClass().getSimpleName());
        }
    }

    private record DeleteFileResult(boolean existed, boolean deleted, String error) {}

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

    // Nested record ist für P0 völlig ok.
    public record StoredFile(Path path, String contentType, String originalFileName, String sha256) {}
}
