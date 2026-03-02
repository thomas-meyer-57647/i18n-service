package de.innologic.i18nservice.translations.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "Runtime translation payload")
public record TranslationsResponse(
        @Schema(description = "Project key used for the translation lookup", example = "portal") String projectKey,
        @Schema(description = "Language that was requested", example = "de-DE") String requestedLanguageCode,
        @Schema(description = "Language that could be resolved and delivered", example = "de-DE") String resolvedLanguageCode,
        @Schema(description = "Fallback language that was used when translations are missing", example = "en-US") String fallbackLanguageCode,
        @Schema(description = "Translations that were resolved for the request", example = "{\"welcome.title\":\"Willkommen\",\"home.cta\":\"Jetzt starten\"}") Map<String, String> values,
        @Schema(description = "Keys that were requested but missing from all available bundles", example = "[\"app.unknown\"]") List<String> missingKeys,
        @Schema(description = "Warnings emitted while resolving translations, e.g., fallback notices", example = "[\"Using fallback language en-US\"]") List<String> warnings
) {}
