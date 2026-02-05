package de.innologic.i18nservice.persistence.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.i18nservice.common.exception.ConflictException;
import de.innologic.i18nservice.common.exception.NotFoundException;
import de.innologic.i18nservice.language.model.LanguageEntity;
import de.innologic.i18nservice.language.repo.LanguageRepository;
import de.innologic.i18nservice.persistence.dto.BundleMetaResponse;
import de.innologic.i18nservice.persistence.model.LanguageBundleEntity;
import de.innologic.i18nservice.persistence.repo.LanguageBundleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;

@Service
public class LanguageBundleService {

    private final LanguageRepository languageRepo;
    private final LanguageBundleRepository bundleRepo;
    private final ObjectMapper objectMapper;

    private final Path basePath;

    public LanguageBundleService(
            LanguageRepository languageRepo,
            LanguageBundleRepository bundleRepo,
            ObjectMapper objectMapper,
            @Value("${app.bundle-storage.base-path:./data/i18n}") String basePath
    ) {
        this.languageRepo = languageRepo;
        this.bundleRepo = bundleRepo;
        this.objectMapper = objectMapper;
        this.basePath = Paths.get(basePath);
    }

    @Transactional
    public BundleMetaResponse upload(String projectKey, String languageCode, MultipartFile file, String actor) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, normalizeCode(languageCode))
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCode));

        // P0: nur JSON akzeptieren
        String originalName = file.getOriginalFilename() == null ? "bundle.json" : file.getOriginalFilename();
        if (!originalName.toLowerCase().endsWith(".json")) {
            throw new IllegalArgumentException("Only .json bundles are supported in P0");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read uploaded file");
        }

        // Validierung: JSON muss flache Map<String,String> sein
        validateJsonBundle(bytes);

        String sha = sha256Hex(bytes);

        // Speichern: ./data/i18n/{projectKey}/{languageCode}/bundle-{timestamp}-{sha}.json
        Path dir = basePath.resolve(projectKey).resolve(normalizeCode(languageCode));
        try {
            Files.createDirectories(dir);
            String safeName = "bundle-" + Instant.now().toEpochMilli() + "-" + sha + ".json";
            Path target = dir.resolve(safeName);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);

            LanguageBundleEntity entity = bundleRepo.findByLanguageId(lang.getId())
                    .orElseGet(() -> {
                        LanguageBundleEntity e = new LanguageBundleEntity();
                        e.setLanguageId(lang.getId());
                        return e;
                    });

            entity.setFileFormat("JSON");
            entity.setOriginalFileName(originalName);
            entity.setContentType(file.getContentType());
            entity.setStoragePath(target.toString());
            entity.setSha256(sha);
            entity.setSizeBytes(bytes.length);
            entity.setUploadedAt(Instant.now());
            entity.setUploadedBy(actor);

            LanguageBundleEntity saved = bundleRepo.save(entity);

            return BundleMetaResponse.from(saved);

        } catch (FileAlreadyExistsException ex) {
            throw new ConflictException("Bundle with same name already exists (retry upload)");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not store bundle file");
        }
    }

    @Transactional(readOnly = true)
    public StoredFile download(String projectKey, String languageCode) {
        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, normalizeCode(languageCode))
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCode));

        LanguageBundleEntity bundle = bundleRepo.findByLanguageId(lang.getId())
                .orElseThrow(() -> new NotFoundException("Bundle not found for language: " + languageCode));

        Path p = Paths.get(bundle.getStoragePath());
        if (!Files.exists(p)) {
            throw new NotFoundException("Bundle file missing on storage");
        }

        return new StoredFile(p, bundle.getContentType(), bundle.getOriginalFileName());
    }

    @Transactional(readOnly = true)
    public BundleMetaResponse getMeta(String projectKey, String languageCode) {
        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, normalizeCode(languageCode))
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCode));

        LanguageBundleEntity bundle = bundleRepo.findByLanguageId(lang.getId())
                .orElseThrow(() -> new NotFoundException("Bundle not found for language: " + languageCode));

        return BundleMetaResponse.from(bundle);
    }

    private void validateJsonBundle(byte[] bytes) {
        try (InputStream is = new java.io.ByteArrayInputStream(bytes)) {
            Map<String, String> map = objectMapper.readValue(is, new TypeReference<>() {});
            if (map.isEmpty()) throw new IllegalArgumentException("Bundle JSON must not be empty");

            // Minimal checks
            for (var e : map.entrySet()) {
                if (e.getKey() == null || e.getKey().isBlank()) {
                    throw new IllegalArgumentException("Bundle contains empty key");
                }
                if (e.getValue() == null) {
                    throw new IllegalArgumentException("Bundle contains null value for key: " + e.getKey());
                }
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JSON bundle (must be flat key->string map)");
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

    private String normalizeCode(String code) {
        if (code == null) return null;
        String c = code.trim();
        String[] parts = c.split("-");
        if (parts.length == 1) return parts[0].toLowerCase();
        if (parts.length == 2) return parts[0].toLowerCase() + "-" + parts[1].toUpperCase();
        return c;
    }

    public record StoredFile(Path path, String contentType, String originalFileName) {}
}
