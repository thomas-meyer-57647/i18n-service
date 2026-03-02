package de.innologic.i18nservice.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(

        @Schema(description = "Unique identifier (project key) that also serves as path segment", example = "portal")
        @NotBlank
        @Size(min = 3, max = 32)
        @Pattern(
                regexp = "^[a-z][a-z0-9-]{2,31}$",
                message = "projectKey must match ^[a-z][a-z0-9-]{2,31}$ (e.g. portal, crm-app)"
        )
        String projectKey,

        @Schema(description = "Friendly display name", example = "Portal")
        @Size(max = 120)
        String displayName

) {}
