package de.innologic.i18nservice.language.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLanguageRequest(
        @Schema(description = "Updated display name of the language", example = "Deutsch")
        @NotBlank
        @Size(max = 120)
        String name,
        @Schema(description = "Activation flag", example = "true")
        boolean active,

        @Schema(description = "Version number for optimistic locking", example = "3")
        long expectedVersion
) {}
