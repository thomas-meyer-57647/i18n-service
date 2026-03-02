package de.innologic.i18nservice.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLanguageSettingsRequest(
        @Schema(description = "Default language code for the project", example = "de-DE")
        @NotBlank
        @Size(max = 35)
        String defaultLanguageCode,

        @Schema(description = "Fallback language code", example = "en-US")
        @Size(max = 35)
        String fallbackLanguageCode,

        @Schema(description = "Optimistic locking version", example = "2")
        long expectedVersion
) {}
