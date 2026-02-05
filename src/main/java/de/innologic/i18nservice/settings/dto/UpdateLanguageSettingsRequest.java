package de.innologic.i18nservice.settings.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateLanguageSettingsRequest(
        @Size(max = 35)
        @Pattern(regexp = "^[a-zA-Z]{2,3}(-[a-zA-Z]{2})?$", message = "Use e.g. de or de-DE")
        String defaultLanguageCode,

        @Size(max = 35)
        @Pattern(regexp = "^[a-zA-Z]{2,3}(-[a-zA-Z]{2})?$", message = "Use e.g. de or de-DE")
        String fallbackLanguageCode,

        long expectedVersion
) {}
