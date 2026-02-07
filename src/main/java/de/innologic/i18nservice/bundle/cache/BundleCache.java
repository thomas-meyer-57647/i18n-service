package de.innologic.i18nservice.bundle.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.i18nservice.bundle.model.LanguageBundleEntity;
import de.innologic.i18nservice.bundle.repo.LanguageBundleRepository;
import de.innologic.i18nservice.common.exception.NotFoundException;
import de.innologic.i18nservice.common.util.LanguageCodeNormalizer;
import de.innologic.i18nservice.language.model.LanguageEntity;
import de.innologic.i18nservice.language.repo.LanguageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Component
public class BundleCache {

    private final LanguageRepository languageRepo;
    private final LanguageBundleRepository bundleRepo;
    private final ObjectMapper objectMapper;

    private final Cache<CacheKey, CacheEntry> cache;

    public BundleCache(
            LanguageRepository languageRepo,
            LanguageBundleRepository bundleRepo,
            ObjectMapper objectMapper,
            @Value("${app.bundle-cache.maximum-size:2000}") long maximumSize,
            @Value("${app.bundle-cache.ttl-seconds:300}") long ttlSeconds
    ) {
        this.languageRepo = languageRepo;
        this.bundleRepo = bundleRepo;
        this.objectMapper = objectMapper;

        this.cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .recordStats()
                .build();
    }

    /**
     * Lädt und cached das Bundle als Map<String,String>.
     * Cache ist an sha256 gebunden: ändert sich sha256 in DB (neuer Upload),
     * wird automatisch neu geladen.
     */
    public Map<String, String> loadBundleMap(String projectKey, String languageCode) {
        String code = normalizeLanguageCode(languageCode);

        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, code)
                .orElseThrow(() -> new NotFoundException("Language not found: " + code));

        LanguageBundleEntity bundle = bundleRepo.findByLanguageId(lang.getId())
                .orElseThrow(() -> new NotFoundException("Bundle not found for language: " + code));

        CacheKey key = new CacheKey(projectKey, code);

        CacheEntry existing = cache.getIfPresent(key);
        if (existing != null && existing.sha256().equals(bundle.getSha256())) {
            return existing.data();
        }

        // neu laden
        Path p = Paths.get(bundle.getStoragePath());
        if (!Files.exists(p)) {
            throw new NotFoundException("Bundle file missing on storage for language: " + code);
        }

        try {
            byte[] bytes = Files.readAllBytes(p);

            // flache JSON Map (P0)
            Map<String, String> map = objectMapper.readValue(bytes, new TypeReference<>() {});
            cache.put(key, new CacheEntry(bundle.getSha256(), map));
            return map;

        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot read/parse bundle JSON for language: " + code);
        }
    }

    /** Manuelles Invalidieren (nach Upload/Replace/Delete/Purge). */
    public void invalidate(String projectKey, String languageCode) {
        String code = normalizeLanguageCode(languageCode);
        cache.invalidate(new CacheKey(projectKey, code));
    }

    /** Optional: alles leeren (z.B. Admin/Tests). */
    public void clear() {
        cache.invalidateAll();
    }

    // Für Micrometer-Binder
    public com.github.benmanes.caffeine.cache.stats.CacheStats stats() {
        return cache.stats();
    }

    public double estimatedSize() {
        return cache.estimatedSize();
    }

    public record CacheKey(String projectKey, String languageCode) {}
    public record CacheEntry(String sha256, Map<String, String> data) {}

    private String normalizeLanguageCode(String raw) {
        String n = LanguageCodeNormalizer.normalize(raw);
        if (n == null || !LanguageCodeNormalizer.isPlausible(n)) {
            throw new IllegalArgumentException("Invalid languageCode. Use e.g. de, de-DE, zh-Hant-TW");
        }
        return n;
    }
}
