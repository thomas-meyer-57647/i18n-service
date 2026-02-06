package de.innologic.i18nservice.bundle.dto;

import java.util.List;

public record BundleDiffResponse(
        int fromVersion,
        int toVersion,
        int addedCount,
        int removedCount,
        int changedCount,
        List<String> addedKeys,
        List<String> removedKeys,
        List<ChangedEntry> changedKeys
) {
    public record ChangedEntry(String key, String fromValue, String toValue) {}
}
