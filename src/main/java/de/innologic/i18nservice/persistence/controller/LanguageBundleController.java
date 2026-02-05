package de.innologic.i18nservice.persistence.controller;
import de.innologic.i18nservice.persistence.dto.BundleMetaResponse;
import de.innologic.i18nservice.persistence.service.LanguageBundleService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 *  language_bundle Tabelle + Entity + Repo
 *
 *  Upload speichert Datei außerhalb der DB
 *  Upload validiert JSON (flache Map)
 *  Download liefert Datei zurück
 *
 *  Meta-Endpunkt liefert Hash/Size/Version etc.
 */
@RestController
@RequestMapping("/api/v1/{projectKey}/languages/{languageCode}/bundle")
public class LanguageBundleController {

    private final LanguageBundleService service;

    public LanguageBundleController(LanguageBundleService service) {
        this.service = service;
    }

    private String actor() { return "system"; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BundleMetaResponse upload(
            @PathVariable String projectKey,
            @PathVariable String languageCode,
            @RequestPart("file") MultipartFile file
    ) {
        return service.upload(projectKey, languageCode, file, actor());
    }

    @GetMapping
    public ResponseEntity<Resource> download(
            @PathVariable String projectKey,
            @PathVariable String languageCode
    ) {
        var stored = service.download(projectKey, languageCode);
        Resource res = new FileSystemResource(stored.path());

        String ct = stored.contentType() != null ? stored.contentType() : MediaType.APPLICATION_JSON_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + stored.originalFileName() + "\"")
                .body(res);
    }

    @GetMapping("/meta")
    public BundleMetaResponse meta(
            @PathVariable String projectKey,
            @PathVariable String languageCode
    ) {
        return service.getMeta(projectKey, languageCode);
    }
}
