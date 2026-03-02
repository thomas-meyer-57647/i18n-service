package de.innologic.i18nservice.bundle.dto;

import de.innologic.i18nservice.bundle.model.LanguageBundleEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record BundleMetaResponse(
        @Schema(description = "Bundle primary key", example = "123")
        Long bundleId,
        @Schema(description = "Language identifier", example = "7")
        Long languageId,
        @Schema(description = "Bundle version number", example = "4")
        int bundleVersion,
        @Schema(description = "File format", example = "json")
        String fileFormat,
        @Schema(description = "Original upload filename", example = "bundle.json")
        String originalFileName,
        @Schema(description = "Content type", example = "application/json")
        String contentType,
        @Schema(description = "SHA-256 checksum", example = "e3b0c44298fc1c149afbf4c8996fb924...")
        String sha256,
        @Schema(description = "Size in bytes", example = "2048")
        long sizeBytes,
        @Schema(description = "Upload timestamp", example = "2024-11-05T09:12:33Z")
        Instant uploadedAt,
        @Schema(description = "Entity version", example = "6")
        long version
) {
    public static BundleMetaResponse from(LanguageBundleEntity e) {
        return new BundleMetaResponse(
                e.getId(),
                e.getLanguageId(),
                e.getBundleVersion(),
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
