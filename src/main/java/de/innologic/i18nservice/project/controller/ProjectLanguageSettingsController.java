package de.innologic.i18nservice.project.controller;

import de.innologic.i18nservice.common.context.RequestContext;
import de.innologic.i18nservice.project.dto.LanguageSettingsResponse;
import de.innologic.i18nservice.project.dto.UpdateLanguageSettingsRequest;
import de.innologic.i18nservice.project.service.ProjectLanguageSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/{projectKey}/language-settings")
@Tag(name = "Language Settings", description = "Inspect and update language fallback settings for a project")
public class ProjectLanguageSettingsController {

    private final ProjectLanguageSettingsService service;

    public ProjectLanguageSettingsController(ProjectLanguageSettingsService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
            summary = "Read language settings",
            description = "Returns the default and fallback language codes for the project.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Language Settings"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Current language settings", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LanguageSettingsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Project not found")
    })
    public LanguageSettingsResponse get(@PathVariable @Parameter(description = "Project key", example = "portal") String projectKey) {
        return service.get(projectKey);
    }

    @PutMapping
    @Operation(
            summary = "Update language settings",
            description = "Replaces the default/fallback language codes for the project. Requires optimistic locking.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Language Settings"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Settings updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LanguageSettingsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "409", description = "Version conflict")
    })
    public LanguageSettingsResponse update(
            @PathVariable @Parameter(description = "Project key", example = "portal") String projectKey,
            @Valid @RequestBody UpdateLanguageSettingsRequest req
    ) {
        return service.update(projectKey, req, RequestContext.actor());
    }
}
