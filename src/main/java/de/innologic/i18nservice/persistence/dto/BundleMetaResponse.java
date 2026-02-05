package de.innologic.i18nservice.persistence.dto;
import de.innologic.i18nservice.persistence.model.LanguageBundleEntity;
import java.time.Instant;

public record BundleMetaResponse(
        Long bundleId,
        Long languageId,
        String fileFormat,
        String originalFileName,
        String contentType,
        String sha256,
        long sizeBytes,
        Instant uploadedAt,
        long version
) {
    public static BundleMetaResponse from(LanguageBundleEntity e) {
        return new BundleMetaResponse(
                e.getId(),
                e.getLanguageId(),
                e.getFileFormat(),
                e.getOriginalFileName(),
                e.getContentType(),
                e.getSha256(),
                e.getSizeBytes(),
                e.getUploadedAt(),
                e.getVersion()
        );
    }
}