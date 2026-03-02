package de.innologic.i18nservice.project.controller;

import de.innologic.i18nservice.project.dto.CreateProjectRequest;
import de.innologic.i18nservice.project.dto.ProjectScopeResponse;
import de.innologic.i18nservice.project.service.ProjectScopeService;
import de.innologic.i18nservice.security.JwtPrincipalAccessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Project Scopes", description = "Manage project scopes (translation bundles) for the current tenant")
public class ProjectScopeController {

    private final ProjectScopeService service;
    private final JwtPrincipalAccessor principalAccessor;

    public ProjectScopeController(ProjectScopeService service, JwtPrincipalAccessor principalAccessor) {
        this.service = service;
        this.principalAccessor = principalAccessor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a project scope",
            description = "Provision a project scope bound to the authenticated tenant.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Project Scopes"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Project scope created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectScopeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payload"),
            @ApiResponse(responseCode = "401", description = "Authentication missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Tenant does not match or insufficient scope"),
            @ApiResponse(responseCode = "409", description = "Project key already exists")
    })
    public ProjectScopeResponse create(@Valid @RequestBody CreateProjectRequest req) {
        return service.create(req, principalAccessor.currentSubject(), principalAccessor.currentTenantId());
    }

    // Optional, aber sehr praktisch (Admin/API-Nutzung):
    @GetMapping
    @Operation(
            summary = "List project scopes for the tenant",
            description = "Returns the scopes the tenant owns.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Project Scopes"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Scopes available to the tenant", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProjectScopeResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Authentication missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Insufficient rights")
    })
    public List<ProjectScopeResponse> list() {
        return service.list(principalAccessor.currentTenantId());
    }

    // Optional:
    @GetMapping("/{projectKey}")
    @Operation(
            summary = "Get details for one project scope",
            description = "Returns the project scope identified by the path segment.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Project Scopes"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Scope found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectScopeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Insufficient rights"),
            @ApiResponse(responseCode = "404", description = "Project scope not found")
    })
    public ProjectScopeResponse get(@PathVariable @Parameter(description = "case-sensitive project key", example = "portal") String projectKey) {
        return service.get(projectKey);
    }
}
