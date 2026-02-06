package de.innologic.i18nservice.common.util;
import java.util.regex.Pattern;

public final class ProjectKeyValidator {
    private static final Pattern P = Pattern.compile("^[a-z][a-z0-9-]{2,31}$");

    private ProjectKeyValidator() {}

    public static void validate(String projectKey) {
        if (projectKey == null || !P.matcher(projectKey).matches()) {
            throw new IllegalArgumentException("Invalid projectKey. Use ^[a-z][a-z0-9-]{2,31}$");
        }
    }
}
