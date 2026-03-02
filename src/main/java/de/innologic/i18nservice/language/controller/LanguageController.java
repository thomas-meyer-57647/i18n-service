package de.innologic.i18nservice.language.controller;

import de.innologic.i18nservice.common.context.RequestContext;
import de.innologic.i18nservice.language.dto.CreateLanguageRequest;
import de.innologic.i18nservice.language.dto.LanguageResponse;
import de.innologic.i18nservice.language.dto.UpdateLanguageRequest;
import de.innologic.i18nservice.language.service.LanguageService;
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
@RequestMapping("/api/v1/{projectKey}/languages")
@Tag(name = "Languages", description = "Manage language metadata within a project scope")
public class LanguageController {

    private final LanguageService service;

    public LanguageController(LanguageService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a language entry",
            description = "Adds a language to the project scope. Requires i18n:admin.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Languages"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Language added", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LanguageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Project not found"),
            @ApiResponse(responseCode = "409", description = "Language already exists")
    })
    public LanguageResponse create(
            @PathVariable @Parameter(description = "Project key (tenant-bound)", example = "portal") String projectKey,
            @Valid @RequestBody CreateLanguageRequest req
    ) {
        return service.create(projectKey, req, RequestContext.actor());
    }

    /**
     * Listing:
     * - default: view=active  -> nur active && !deleted
     * - view=all             -> alle (active+inactive, deleted+not deleted)
     * - view=deleted         -> nur deleted
     */
    @GetMapping
    @Operation(
            summary = "List languages for a project",
            description = "Returns active, all, or deleted languages depending on the `view` parameter.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Languages"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of languages", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = LanguageResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid view parameter"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public List<LanguageResponse> list(
            @PathVariable @Parameter(description = "Project key (tenant-bound)", example = "portal") String projectKey,
            @Parameter(description = "View filter: active | all | deleted", example = "active")
            @RequestParam(name = "view", defaultValue = "active") String view
    ) {
        return switch (view.toLowerCase()) {
            case "active" -> service.listActive(projectKey);
            case "all" -> service.listAll(projectKey);
            case "deleted" -> service.listDeleted(projectKey);
            default -> throw new IllegalArgumentException("Invalid view. Use: active | all | deleted");
        };
    }

    @GetMapping("/{languageCode}")
    @Operation(
            summary = "Get a language by code",
            description = "Returns the language metadata for the provided language code.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Languages"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Language found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LanguageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Language not found")
    })
    public LanguageResponse get(
            @PathVariable @Parameter(description = "Project key (tenant-bound)", example = "portal") String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") String languageCode
    ) {
        return service.get(projectKey, languageCode);
    }

    @PutMapping("/{languageCode}")
    @Operation(
            summary = "Update a language",
            description = "Updates name/activation state using optimistic locking.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Languages"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Language updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LanguageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Language not found"),
            @ApiResponse(responseCode = "409", description = "Version conflict")
    })
    public LanguageResponse update(
            @PathVariable @Parameter(description = "Project key", example = "portal") String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") String languageCode,
            @Valid @RequestBody UpdateLanguageRequest req
    ) {
        return service.update(projectKey, languageCode, req, RequestContext.actor());
    }

    /**
     * Soft delete -> Papierkorb
     */
    @DeleteMapping("/{languageCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Soft delete a language",
            description = "Moves the language to the trash while keeping metadata for potential restore.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Languages"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Language soft deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Language not found")
    })
    public void softDelete(
            @PathVariable @Parameter(description = "Project key", example = "portal") String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") String languageCode
    ) {
        service.softDelete(projectKey, languageCode, RequestContext.actor());
    }

    @PostMapping("/{languageCode}/restore")
    @Operation(
            summary = "Restore a soft-deleted language",
            description = "Reactivates a language from the trash.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Languages"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Language restored", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LanguageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Language not found")
    })
    public LanguageResponse restore(
            @PathVariable @Parameter(description = "Project key", example = "portal") String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") String languageCode
    ) {
        return service.restore(projectKey, languageCode, RequestContext.actor());
    }

    /**
     * Hard delete (Purge)
     * - ohne force nur möglich, wenn die Sprache bereits deleted=true ist
     * - mit force=true auch möglich, wenn nicht deleted
     */
    @DeleteMapping("/{languageCode}/purge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Purge a language",
            description = "Hard deletes the language; use force=true to purge active entries.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Languages"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Language purged"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Language not found"),
            @ApiResponse(responseCode = "409", description = "Cannot purge unless deleted (force=false)")
    })
    public void purge(
            @PathVariable @Parameter(description = "Project key", example = "portal") String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") String languageCode,
            @Parameter(description = "Force purge even when not deleted", example = "false")
            @RequestParam(name = "force", defaultValue = "false") boolean force
    ) {
        service.purge(projectKey, languageCode, force, RequestContext.actor());
    }
}
