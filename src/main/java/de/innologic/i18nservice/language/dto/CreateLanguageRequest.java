package de.innologic.i18nservice.language.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLanguageRequest(
        @Schema(description = "Locale name displayed to translators", example = "Deutsch")
        @NotBlank
        @Size(max = 120)
        String name,

        @Schema(description = "Language code in BCP47 format", example = "de-DE")
        @NotBlank
        @Size(max = 35)
        String languageCode
) {}

