package de.innologic.i18nservice.language.dto;

import de.innologic.i18nservice.language.model.LanguageEntity;
import io.swagger.v3.oas.annotations.media.Schema;

public record LanguageResponse(
        @Schema(description = "Database identifier", example = "42")
        Long id,
        @Schema(description = "Project key", example = "portal")
        String projectKey,
        @Schema(description = "Language code", example = "de-DE")
        String languageCode,
        @Schema(description = "Language name", example = "Deutsch")
        String name,
        @Schema(description = "Activation flag", example = "true")
        boolean active,
        @Schema(description = "Deletion flag", example = "false")
        boolean deleted,
        @Schema(description = "Version for optimistic locking", example = "5")
        long version
) {
    public static LanguageResponse from(LanguageEntity e) {
        return new LanguageResponse(
                e.getId(), e.getProjectKey(), e.getLanguageCode(), e.getName(),
                e.isActive(), e.isDeleted(), e.getVersion()
        );
    }
}
