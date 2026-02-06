package de.innologic.i18nservice.language.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLanguageRequest(
        @NotBlank @Size(max = 120) String name,

        @NotBlank
        @Size(max = 35)
        String languageCode
) {}

