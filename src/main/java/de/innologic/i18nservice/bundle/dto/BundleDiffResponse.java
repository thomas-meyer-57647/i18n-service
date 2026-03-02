package de.innologic.i18nservice.bundle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Comparison between two bundle versions")
public record BundleDiffResponse(
        @Schema(description = "Starting bundle version number", example = "1") int fromVersion,
        @Schema(description = "Target bundle version number", example = "2") int toVersion,
        @Schema(description = "Number of keys added in the target version", example = "5") int addedCount,
        @Schema(description = "Number of keys removed in the target version", example = "2") int removedCount,
        @Schema(description = "Number of keys whose values changed", example = "3") int changedCount,
        @Schema(description = "Keys that were added in the target version", example = "[\"welcome.title\"]") List<String> addedKeys,
        @Schema(description = "Keys that were removed in the target version", example = "[\"legacy.label\"]") List<String> removedKeys,
        @Schema(description = "Keys that changed values between versions") List<ChangedEntry> changedKeys
) {
    @Schema(description = "Key with its before/after values")
    public record ChangedEntry(
            @Schema(description = "Translation key that changed", example = "welcome.title") String key,
            @Schema(description = "Value in the starting version", example = "Hello") String fromValue,
            @Schema(description = "Value in the target version", example = "Welcome") String toValue
    ) {}
}
