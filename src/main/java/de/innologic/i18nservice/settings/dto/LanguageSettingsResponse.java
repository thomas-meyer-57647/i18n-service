package de.innologic.i18nservice.settings.dto;

public record LanguageSettingsResponse(
        String projectKey,
        String defaultLanguageCode,
        String fallbackLanguageCode,
        long version
) {}

