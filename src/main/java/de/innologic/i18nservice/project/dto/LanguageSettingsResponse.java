package de.innologic.i18nservice.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LanguageSettingsResponse(
        @Schema(description = "Project key", example = "portal")
        String projectKey,
        @Schema(description = "Default language code", example = "de-DE")
        String defaultLanguageCode,
        @Schema(description = "Fallback language code", example = "en-US")
        String fallbackLanguageCode,
        @Schema(description = "Version for optimistic locking", example = "3")
        long version
) {}

