package de.innologic.i18nservice.language.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLanguageRequest(
        @NotBlank @Size(max = 120) String name,
        boolean active,

        // Konflikte sauber behandeln
        long expectedVersion
) {}
