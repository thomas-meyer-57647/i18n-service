package de.innologic.i18nservice.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLanguageSettingsRequest(
        @NotBlank
        @Size(max = 35)
        String defaultLanguageCode,

        @Size(max = 35)
        String fallbackLanguageCode,

        long expectedVersion
) {}
