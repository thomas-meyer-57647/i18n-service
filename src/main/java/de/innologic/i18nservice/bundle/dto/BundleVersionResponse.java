package de.innologic.i18nservice.bundle.dto;

import de.innologic.i18nservice.bundle.model.LanguageBundleVersionEntity;

import java.time.Instant;

public record BundleVersionResponse(
        Long bundleVersionId,
        int bundleVersion,
        String fileFormat,
        String originalFileName,
        String contentType,
        String sha256,
        long sizeBytes,
        Instant uploadedAt,
        String uploadedBy,
        boolean current
) {
    public static BundleVersionResponse from(LanguageBundleVersionEntity e, int currentVersion) {
        return new BundleVersionResponse(
                e.getId(),
                e.getBundleVersion(),
                e.getFileFormat(),
                e.getOriginalFileName(),
                e.getContentType(),
                e.getSha256(),
                e.getSizeBytes(),
                e.getUploadedAt(),
                e.getUploadedBy(),
                e.getBundleVersion() == currentVersion
        );
    }
}
