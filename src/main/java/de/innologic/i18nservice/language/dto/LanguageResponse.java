package de.innologic.i18nservice.language.dto;

import de.innologic.i18nservice.language.model.LanguageEntity;

public record LanguageResponse(
        Long id,
        String projectKey,
        String languageCode,
        String name,
        boolean active,
        boolean deleted,
        long version
) {
    public static LanguageResponse from(LanguageEntity e) {
        return new LanguageResponse(
                e.getId(), e.getProjectKey(), e.getLanguageCode(), e.getName(),
                e.isActive(), e.isDeleted(), e.getVersion()
        );
    }
}
