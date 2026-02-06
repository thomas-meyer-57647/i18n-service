package de.innologic.i18nservice.translations.service;

import de.innologic.i18nservice.bundle.cache.BundleCache;
import de.innologic.i18nservice.common.exception.ConflictException;
import de.innologic.i18nservice.common.exception.NotFoundException;
import de.innologic.i18nservice.common.util.LanguageCodeNormalizer;
import de.innologic.i18nservice.common.util.ProjectKeyValidator;
import de.innologic.i18nservice.language.model.LanguageEntity;
import de.innologic.i18nservice.language.repo.LanguageRepository;
import de.innologic.i18nservice.project.model.ProjectScopeEntity;
import de.innologic.i18nservice.project.repo.ProjectScopeRepository;
import de.innologic.i18nservice.translations.dto.TranslationsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TranslationService {

    private static final int MAX_KEYS = 500;

    private final ProjectScopeRepository scopeRepo;
    private final LanguageRepository languageRepo;
    private final BundleCache bundleCache;

    public TranslationService(ProjectScopeRepository scopeRepo, LanguageRepository languageRepo, BundleCache bundleCache) {
        this.scopeRepo = scopeRepo;
        this.languageRepo = languageRepo;
        this.bundleCache = bundleCache;
    }

    /**
     * 3 Modi:
     * - keysCsv != null => nur diese Keys (Bulk)
     * - prefix != null  => alle Keys die mit prefix anfangen
     * - sonst           => komplettes Bundle
     */
    @Transactional(readOnly = true)
    public TranslationsResponse getTranslations(
            String projectKey,
            String requestedLanguageCodeRaw,
            String keysCsv,
            String prefix
    ) {
        ProjectKeyValidator.validate(projectKey);

        ProjectScopeEntity scope = scopeRepo.findByProjectKey(projectKey)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectKey));

        String requested = normalizeLangOrThrow(requestedLanguageCodeRaw);
        String resolved = resolveEffectiveLanguageCode(projectKey, scope, requested);

        String fallback = normalizeLangNullable(scope.getFallbackLanguageCode());
        String def = normalizeLangNullable(scope.getDefaultLanguageCode()); // nur für Warnungen

        List<String> warnings = new ArrayList<>();
        if (!Objects.equals(requested, resolved)) {
            warnings.add("Requested language not available/active. Used default: " + resolved);
        }
        if (def == null) {
            // sollte nicht passieren, aber falls Projekt falsch konfiguriert ist
            warnings.add("Default language is not set for project: " + projectKey);
        }

        // Primär-Bundle muss existieren, sonst ist das Projekt nicht lauffähig
        Map<String, String> primaryMap = bundleCache.loadBundleMap(projectKey, resolved);

        // Fallback-Bundle optional
        Map<String, String> fallbackMap = null;
        if (fallback != null && !fallback.equals(resolved)) {
            try {
                fallbackMap = bundleCache.loadBundleMap(projectKey, fallback);
            } catch (RuntimeException ex) {
                warnings.add("Fallback bundle not available: " + fallback);
                fallbackMap = null;
            }
        }

        // Modus bestimmen
        if (keysCsv != null && !keysCsv.isBlank()) {
            return byKeys(projectKey, requested, resolved, fallback, keysCsv, primaryMap, fallbackMap, warnings);
        }
        if (prefix != null && !prefix.isBlank()) {
            return byPrefix(projectKey, requested, resolved, fallback, prefix.trim(), primaryMap, fallbackMap, warnings);
        }

        // Standard: komplettes Bundle
        return fullBundle(projectKey, requested, resolved, fallback, primaryMap, warnings);
    }

    private TranslationsResponse byKeys(
            String projectKey,
            String requested,
            String resolved,
            String fallback,
            String keysCsv,
            Map<String, String> primaryMap,
            Map<String, String> fallbackMap,
            List<String> warnings
    ) {
        List<String> keys = parseKeys(keysCsv);
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("Query param 'keys' is required, e.g. ?keys=A,B,C");
        }
        if (keys.size() > MAX_KEYS) {
            throw new IllegalArgumentException("Too many keys. Max allowed: " + MAX_KEYS);
        }

        Map<String, String> values = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();

        for (String k : keys) {
            String v = primaryMap.get(k);
            if (v == null && fallbackMap != null) v = fallbackMap.get(k);

            if (v == null) {
                missing.add(k);
                v = k; // P0: Key zurückgeben, wenn nicht gefunden
            }
            values.put(k, v);
        }

        return new TranslationsResponse(projectKey, requested, resolved, fallback, values, missing, warnings);
    }

    private TranslationsResponse byPrefix(
            String projectKey,
            String requested,
            String resolved,
            String fallback,
            String prefix,
            Map<String, String> primaryMap,
            Map<String, String> fallbackMap,
            List<String> warnings
    ) {
        // Alle Keys aus primary, die mit prefix beginnen
        // Wenn ein key fehlt im primary (gibt es hier nicht), könnte fallback ergänzen – optional.
        Map<String, String> values = new LinkedHashMap<>();

        primaryMap.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().startsWith(prefix))
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> values.put(e.getKey(), e.getValue()));

        // Optional: Keys aus fallback ergänzen, die primary nicht hat
        if (fallbackMap != null) {
            fallbackMap.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getKey().startsWith(prefix))
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> values.putIfAbsent(e.getKey(), e.getValue()));
        }

        return new TranslationsResponse(projectKey, requested, resolved, fallback, values, List.of(), warnings);
    }

    private TranslationsResponse fullBundle(
            String projectKey,
            String requested,
            String resolved,
            String fallback,
            Map<String, String> primaryMap,
            List<String> warnings
    ) {
        // Komplettes Bundle: 1:1 primaryMap zurückgeben (sortiert, damit stabil)
        Map<String, String> values = primaryMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return new TranslationsResponse(projectKey, requested, resolved, fallback, values, List.of(), warnings);
    }

    private String resolveEffectiveLanguageCode(String projectKey, ProjectScopeEntity scope, String requested) {
        // Wenn angefragte Sprache existiert + aktiv + nicht deleted => ok, sonst Default
        Optional<LanguageEntity> reqLang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, requested);

        if (reqLang.isPresent() && reqLang.get().isActive()) {
            return requested;
        }

        String def = normalizeLangNullable(scope.getDefaultLanguageCode());
        if (def == null) {
            throw new ConflictException("No default language configured for project: " + projectKey);
        }

        Optional<LanguageEntity> defLang = languageRepo
                .findByProjectKeyAndLanguageCodeAndDeletedFalse(projectKey, def);

        if (defLang.isEmpty() || !defLang.get().isActive()) {
            throw new ConflictException("Default language is missing or inactive: " + def);
        }

        return def;
    }

    private List<String> parseKeys(String keysCsv) {
        return Arrays.stream(keysCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalizeLangOrThrow(String raw) {
        String n = LanguageCodeNormalizer.normalize(raw);
        if (n == null || !LanguageCodeNormalizer.isPlausible(n)) {
            throw new IllegalArgumentException("Invalid languageCode. Use e.g. de or de-DE");
        }
        return n;
    }

    private String normalizeLangNullable(String raw) {
        if (raw == null) return null;
        String n = LanguageCodeNormalizer.normalize(raw);
        return (n != null && LanguageCodeNormalizer.isPlausible(n)) ? n : null;
    }
}
