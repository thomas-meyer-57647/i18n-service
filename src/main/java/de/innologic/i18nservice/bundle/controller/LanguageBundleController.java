package de.innologic.i18nservice.bundle.controller;

import de.innologic.i18nservice.bundle.dto.BundleDiffResponse;
import de.innologic.i18nservice.bundle.dto.BundleMetaResponse;
import de.innologic.i18nservice.bundle.dto.BundleVersionResponse;
import de.innologic.i18nservice.bundle.service.LanguageBundleService;
import de.innologic.i18nservice.bundle.service.LanguageBundleService.StoredFile;
import de.innologic.i18nservice.common.context.RequestContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/{projectKey}/languages/{languageCode}/bundle")
@Tag(name = "Language Bundles", description = "Upload/download bundles, list versions, and manage snapshots")
public class LanguageBundleController {

    private final LanguageBundleService service;

    public LanguageBundleController(LanguageBundleService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Upload bundle",
            description = "Uploads a new bundle file for the language.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Language Bundles"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bundle stored", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BundleMetaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing file or invalid payload"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Project or language not found"),
            @ApiResponse(responseCode = "409", description = "Bundle version conflict")
    })
    public BundleMetaResponse upload(
            @PathVariable @Parameter(description = "Project key", example = "portal") @NotBlank String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") @NotBlank String languageCode,
            @RequestPart("file") @NotNull MultipartFile file
    ) {
        return service.upload(projectKey, languageCode, file, RequestContext.actor());
    }

    /**
     * Download AKTUELLES Bundle (mit ETag / If-None-Match -> 304)
     */
    @GetMapping
    @Operation(
            summary = "Download latest bundle",
            description = "Streams the latest bundle with ETag support.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Language Bundles"},
            parameters = {
                    @Parameter(name = HttpHeaders.IF_NONE_MATCH, in = ParameterIn.HEADER, description = "ETag value from previous download")
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bundle content returned"),
            @ApiResponse(responseCode = "304", description = "Client already has latest bundle"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Language bundle not found")
    })
    public ResponseEntity<Resource> download(
            @PathVariable @Parameter(description = "Project key", example = "portal") @NotBlank String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") @NotBlank String languageCode,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        StoredFile stored = service.download(projectKey, languageCode);
        return buildDownloadResponse(stored, ifNoneMatch);
    }

    /**
     * Download einer KONKRETEN Version (mit ETag / If-None-Match -> 304)
     */
    @GetMapping("/version/{version}")
    @Operation(
            summary = "Download specific bundle version",
            description = "Streams the requested bundle version with ETag support.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Language Bundles"},
            parameters = {
                    @Parameter(name = HttpHeaders.IF_NONE_MATCH, in = ParameterIn.HEADER, description = "ETag value")
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bundle content returned"),
            @ApiResponse(responseCode = "304", description = "Client already has that version"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Version not found")
    })
    public ResponseEntity<Resource> downloadVersion(
            @PathVariable @Parameter(description = "Project key", example = "portal") @NotBlank String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") @NotBlank String languageCode,
            @PathVariable("version") @Min(1) int version,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        StoredFile stored = service.downloadVersion(projectKey, languageCode, version);
        return buildDownloadResponse(stored, ifNoneMatch);
    }

    @GetMapping("/meta")
    @Operation(
            summary = "Read bundle metadata",
            description = "Returns metadata for the active bundle.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Language Bundles"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metadata retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BundleMetaResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Language bundle not found")
    })
    public BundleMetaResponse meta(
            @PathVariable @Parameter(description = "Project key", example = "portal") @NotBlank String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") @NotBlank String languageCode
    ) {
        return service.getMeta(projectKey, languageCode);
    }

    /**
     * Meta einer KONKRETEN Version (ohne Download).
     */
    @GetMapping("/version/{version}/meta")
    @Operation(
            summary = "Read metadata for a bundle version",
            description = "Returns metadata for the requested version without downloading.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Language Bundles"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Version metadata", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BundleVersionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Version not found")
    })
    public BundleVersionResponse versionMeta(
            @PathVariable @Parameter(description = "Project key", example = "portal") @NotBlank String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") @NotBlank String languageCode,
            @PathVariable("version") @Parameter(description = "Version number", example = "2") @Min(1) int version
    ) {
        return service.getVersionMeta(projectKey, languageCode, version);
    }

    /**
     * Versionsliste (neueste zuerst).
     */
    @GetMapping("/versions")
    @Operation(
            summary = "List bundle versions",
            description = "Returns all versions (latest first).",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Language Bundles"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Version list", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = BundleVersionResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public java.util.List<BundleVersionResponse> versions(
            @PathVariable @Parameter(description = "Project key", example = "portal") @NotBlank String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") @NotBlank String languageCode
    ) {
        return service.listVersions(projectKey, languageCode);
    }

    /**
     * Rollback auf eine bestimmte Bundle-Version.
     */
    @PostMapping("/rollback/{version}")
    @Operation(
            summary = "Rollback bundle",
            description = "Rolls back to the given version.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Language Bundles"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rolled back bundle", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BundleMetaResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Version not found")
    })
    public BundleMetaResponse rollback(
            @PathVariable @Parameter(description = "Project key", example = "portal") @NotBlank String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") @NotBlank String languageCode,
            @PathVariable("version") @Parameter(description = "Version to rollback to", example = "3") @Min(1) int version
    ) {
        return service.rollback(projectKey, languageCode, version, RequestContext.actor());
    }

    /**
     * Diff zwischen zwei Bundle-Versionen.
     */
    @GetMapping("/diff")
    @Operation(
            summary = "Diff bundle versions",
            description = "Returns the diff between two versions.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Language Bundles"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Diff returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BundleDiffResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "One of the versions not found")
    })
    public BundleDiffResponse diff(
            @PathVariable @Parameter(description = "Project key", example = "portal") @NotBlank String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") @NotBlank String languageCode,
            @RequestParam(name = "from") @Parameter(description = "Starting bundle version", example = "1") @Min(1) int fromVersion,
            @RequestParam(name = "to") @Parameter(description = "Target bundle version", example = "2") @Min(1) int toVersion
    ) {
        return service.diff(projectKey, languageCode, fromVersion, toVersion);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete bundle",
            description = "Removes the current bundle.",
            security = @SecurityRequirement(name = "bearerAuth"),
            tags = {"Language Bundles"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bundle deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Bundle not found")
    })
    public void delete(
            @PathVariable @Parameter(description = "Project key", example = "portal") @NotBlank String projectKey,
            @PathVariable @Parameter(description = "Language code", example = "de-DE") @NotBlank String languageCode
    ) {
        service.deleteBundle(projectKey, languageCode, RequestContext.actor());
    }

    private ResponseEntity<Resource> buildDownloadResponse(StoredFile stored, String ifNoneMatch) {
        String ct = stored.contentType();
        MediaType mediaType = (ct != null && !ct.isBlank())
                ? MediaType.parseMediaType(ct)
                : MediaType.APPLICATION_JSON;

        String filename = (stored.originalFileName() != null && !stored.originalFileName().isBlank())
                ? stored.originalFileName()
                : "bundle.json";

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename)
                .build();

        // ETag muss quoted sein
        String etag = "\"" + stored.sha256() + "\"";

        // 304 wenn Client das gleiche schon hat
        if (etagMatches(ifNoneMatch, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(CacheControl.noCache().cachePrivate())
                    .build();
        }

        Resource resource = new FileSystemResource(stored.path());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .eTag(etag)
                .cacheControl(CacheControl.noCache().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    private boolean etagMatches(String ifNoneMatch, String etag) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank()) return false;

        // "*" bedeutet: match
        if (ifNoneMatch.trim().equals("*")) return true;

        // Header kann mehrere Werte enthalten, auch weak ETags (W/"...")
        String[] parts = ifNoneMatch.split(",");
        for (String p : parts) {
            String candidate = p.trim();
            if (candidate.equals(etag)) return true;
            if (candidate.startsWith("W/") && candidate.substring(2).trim().equals(etag)) return true;
        }
        return false;
    }
}
