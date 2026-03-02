package de.innologic.i18nservice.bundle.dto;

import de.innologic.i18nservice.bundle.model.LanguageBundleVersionEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record BundleVersionResponse(
        @Schema(description = "Bundle version record id", example = "55")
        Long bundleVersionId,
        @Schema(description = "Version number", example = "3")
        int bundleVersion,
        @Schema(description = "File format", example = "json")
        String fileFormat,
        @Schema(description = "Original file name", example = "bundle-v3.json")
        String originalFileName,
        @Schema(description = "Content type", example = "application/json")
        String contentType,
        @Schema(description = "SHA-256 checksum", example = "aaaabbbbccccddddeeeeffff11112222")
        String sha256,
        @Schema(description = "Size in bytes", example = "2048")
        long sizeBytes,
        @Schema(description = "Upload timestamp", example = "2024-11-05T09:12:33Z")
        Instant uploadedAt,
        @Schema(description = "Actor who uploaded", example = "user-42")
        String uploadedBy,
        @Schema(description = "Latest version flag", example = "true")
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
