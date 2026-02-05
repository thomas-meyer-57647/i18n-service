package de.innologic.i18nservice.language.dto;

import de.innologic.i18nservice.language.model.LanguageEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLanguageRequest(
        @NotBlank @Size(max = 120) String name,

        @NotBlank
        @Size(max = 35)
        // minimaler BCP-47 Check (für de, de-DE, en-US)
        @Pattern(regexp = "^[a-zA-Z]{2,3}(-[a-zA-Z]{2})?$", message = "Use e.g. de or de-DE")
        String languageCode
) {}

