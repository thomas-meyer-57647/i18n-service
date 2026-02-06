package de.innologic.i18nservice.project.dto;

public record LanguageSettingsResponse(
        String projectKey,
        String defaultLanguageCode,
        String fallbackLanguageCode,
        long version
) {}

