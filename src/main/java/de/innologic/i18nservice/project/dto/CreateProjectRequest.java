package de.innologic.i18nservice.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(

        @NotBlank
        @Size(min = 3, max = 32)
        @Pattern(
                regexp = "^[a-z][a-z0-9-]{2,31}$",
                message = "projectKey must match ^[a-z][a-z0-9-]{2,31}$ (e.g. portal, crm-app)"
        )
        String projectKey,

        @Size(max = 120)
        String displayName

) {}
