package de.innologic.i18nservice.bundle.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.innologic.i18nservice.bundle.model.LanguageBundleEntity;
import de.innologic.i18nservice.bundle.repo.LanguageBundleRepository;
import de.innologic.i18nservice.common.exception.NotFoundException;
import de.innologic.i18nservice.language.model.LanguageEntity;
import de.innologic.i18nservice.language.repo.LanguageRepository;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BundleCache {

    private final LanguageRepository languageRepo;
    private final LanguageBundleRepository bundleRepo;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public BundleCache(LanguageRepository languageRepo, LanguageBundleRepository bundleRepo, ObjectMapper objectMapper) {
        this.languageRepo = languageRepo;
        this.bundleRepo = bundleRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * Lädt und cached das Bundle als Map<String,String>.
     * Cache ist an sha256 gebunden: ändert sich sha256 in DB (neuer Upload),
     * wird automatisch neu geladen.
     */
    public Map<String, String> loadBundleMap(String projectKey, String languageCode) {
        LanguageEntity lang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, languageCode)
                .orElseThrow(() -> new NotFoundException("Language not found: " + languageCode));

        LanguageBundleEntity bundle = bundleRepo.findByLanguageId(lang.getId())
                .orElseThrow(() -> new NotFoundException("Bundle not found for language: " + languageCode));

        CacheKey key = new CacheKey(projectKey, languageCode);

        CacheEntry existing = cache.get(key);
        if (existing != null && existing.sha256().equals(bundle.getSha256())) {
            return existing.data();
        }

        // neu laden
        Path p = Paths.get(bundle.getStoragePath());
        if (!Files.exists(p)) {
            throw new NotFoundException("Bundle file missing on storage for language: " + languageCode);
        }

        try {
            byte[] bytes = Files.readAllBytes(p);

            // flache JSON Map (P0)
            Map<String, String> map = objectMapper.readValue(bytes, new TypeReference<>() {});
            cache.put(key, new CacheEntry(bundle.getSha256(), map));
            return map;

        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot read/parse bundle JSON for language: " + languageCode);
        }
    }

    /** Manuelles Invalidieren (nach Upload/Replace/Delete/Purge). */
    public void invalidate(String projectKey, String languageCode) {
        cache.remove(new CacheKey(projectKey, languageCode));
    }

    /** Optional: alles leeren (z.B. Admin/Tests). */
    public void clear() {
        cache.clear();
    }

    public record CacheKey(String projectKey, String languageCode) {}
    public record CacheEntry(String sha256, Map<String, String> data) {}
}
