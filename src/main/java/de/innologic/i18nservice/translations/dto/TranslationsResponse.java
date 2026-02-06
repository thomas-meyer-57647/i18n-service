package de.innologic.i18nservice.translations.dto;

import java.util.List;
import java.util.Map;

public record TranslationsResponse(
        String projectKey,
        String requestedLanguageCode,
        String resolvedLanguageCode,
        String fallbackLanguageCode,
        Map<String, String> values,
        List<String> missingKeys,
        List<String> warnings
) {}
