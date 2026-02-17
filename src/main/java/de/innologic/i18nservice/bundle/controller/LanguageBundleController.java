package de.innologic.i18nservice.bundle.controller;

import de.innologic.i18nservice.bundle.dto.BundleDiffResponse;
import de.innologic.i18nservice.bundle.dto.BundleMetaResponse;
import de.innologic.i18nservice.bundle.dto.BundleVersionResponse;
import de.innologic.i18nservice.bundle.service.LanguageBundleService;
import de.innologic.i18nservice.bundle.service.LanguageBundleService.StoredFile;
import de.innologic.i18nservice.common.context.RequestContext;
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
public class LanguageBundleController {

    private final LanguageBundleService service;

    public LanguageBundleController(LanguageBundleService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BundleMetaResponse upload(
            @PathVariable @NotBlank String projectKey,
            @PathVariable @NotBlank String languageCode,
            @RequestPart("file") @NotNull MultipartFile file
    ) {
        return service.upload(projectKey, languageCode, file, RequestContext.actor());
    }

    /**
     * Download AKTUELLES Bundle (mit ETag / If-None-Match -> 304)
     */
    @GetMapping
    public ResponseEntity<Resource> download(
            @PathVariable @NotBlank String projectKey,
            @PathVariable @NotBlank String languageCode,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        StoredFile stored = service.download(projectKey, languageCode);
        return buildDownloadResponse(stored, ifNoneMatch);
    }

    /**
     * Download einer KONKRETEN Version (mit ETag / If-None-Match -> 304)
     */
    @GetMapping("/version/{version}")
    public ResponseEntity<Resource> downloadVersion(
            @PathVariable @NotBlank String projectKey,
            @PathVariable @NotBlank String languageCode,
            @PathVariable("version") @Min(1) int version,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        StoredFile stored = service.downloadVersion(projectKey, languageCode, version);
        return buildDownloadResponse(stored, ifNoneMatch);
    }

    @GetMapping("/meta")
    public BundleMetaResponse meta(
            @PathVariable @NotBlank String projectKey,
            @PathVariable @NotBlank String languageCode
    ) {
        return service.getMeta(projectKey, languageCode);
    }

    /**
     * Meta einer KONKRETEN Version (ohne Download).
     */
    @GetMapping("/version/{version}/meta")
    public BundleVersionResponse versionMeta(
            @PathVariable @NotBlank String projectKey,
            @PathVariable @NotBlank String languageCode,
            @PathVariable("version") @Min(1) int version
    ) {
        return service.getVersionMeta(projectKey, languageCode, version);
    }

    /**
     * Versionsliste (neueste zuerst).
     */
    @GetMapping("/versions")
    public java.util.List<BundleVersionResponse> versions(
            @PathVariable @NotBlank String projectKey,
            @PathVariable @NotBlank String languageCode
    ) {
        return service.listVersions(projectKey, languageCode);
    }

    /**
     * Rollback auf eine bestimmte Bundle-Version.
     */
    @PostMapping("/rollback/{version}")
    public BundleMetaResponse rollback(
            @PathVariable @NotBlank String projectKey,
            @PathVariable @NotBlank String languageCode,
            @PathVariable("version") @Min(1) int version
    ) {
        return service.rollback(projectKey, languageCode, version, RequestContext.actor());
    }

    /**
     * Diff zwischen zwei Bundle-Versionen.
     */
    @GetMapping("/diff")
    public BundleDiffResponse diff(
            @PathVariable @NotBlank String projectKey,
            @PathVariable @NotBlank String languageCode,
            @RequestParam(name = "from") @Min(1) int fromVersion,
            @RequestParam(name = "to") @Min(1) int toVersion
    ) {
        return service.diff(projectKey, languageCode, fromVersion, toVersion);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable @NotBlank String projectKey,
            @PathVariable @NotBlank String languageCode
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
