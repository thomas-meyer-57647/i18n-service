package de.innologic.i18nservice.project.dto;

import de.innologic.i18nservice.project.model.ProjectScopeEntity;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectScopeResponse(
        @Schema(description = "Unique project key used as identifier", example = "portal")
        String projectKey,
        @Schema(description = "Human-readable name", example = "Portal")
        String displayName,
        @Schema(description = "Default language code for bundles", example = "de-DE")
        String defaultLanguageCode,
        @Schema(description = "Fallback language code if translation missing", example = "en-US")
        String fallbackLanguageCode,
        @Schema(description = "Numerical version of the scope metadata", example = "6")
        long version
) {
    public static ProjectScopeResponse from(ProjectScopeEntity e) {
        return new ProjectScopeResponse(
                e.getProjectKey(),
                e.getDisplayName(),
                e.getDefaultLanguageCode(),
                e.getFallbackLanguageCode(),
                e.getVersion()
        );
    }
}
