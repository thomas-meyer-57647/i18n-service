package de.innologic.i18nservice.project.dto;

import de.innologic.i18nservice.project.model.ProjectScopeEntity;

public record ProjectScopeResponse(
        String projectKey,
        String displayName,
        String defaultLanguageCode,
        String fallbackLanguageCode,
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
