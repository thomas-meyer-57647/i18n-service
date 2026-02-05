package de.innologic.i18nservice.language.controller;

import de.innologic.i18nservice.language.dto.CreateLanguageRequest;
import de.innologic.i18nservice.language.dto.LanguageResponse;
import de.innologic.i18nservice.language.dto.UpdateLanguageRequest;
import de.innologic.i18nservice.language.service.LanguageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 language Tabelle mit project_key, language_code, row_version, is_deleted/deleted_at

 Unique: (project_key, language_code, is_deleted)

 Endpoints laufen:
 POST /api/v1/{projectKey}/languages
 GET /api/v1/{projectKey}/languages
 GET /api/v1/{projectKey}/languages/{languageCode}
 PUT /api/v1/{projectKey}/languages/{languageCode} (mit expectedVersion)
 DELETE /api/v1/{projectKey}/languages/{languageCode} (soft delete)
 POST /api/v1/{projectKey}/languages/{languageCode}/restore

 */
@RestController
@RequestMapping("/api/v1/{projectKey}/languages")
public class LanguageController {

    private final LanguageService service;

    public LanguageController(LanguageService service) {
        this.service = service;
    }

    // später ggf. aus IAM/JWT ziehen
    private String actor() { return "system"; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LanguageResponse create(
            @PathVariable String projectKey,
            @Valid @RequestBody CreateLanguageRequest req
    ) {
        return service.create(projectKey, req, actor());
    }

    @GetMapping
    public List<LanguageResponse> list(@PathVariable String projectKey) {
        return service.list(projectKey);
    }

    @GetMapping("/{languageCode}")
    public LanguageResponse get(@PathVariable String projectKey, @PathVariable String languageCode) {
        return service.get(projectKey, languageCode);
    }

    @PutMapping("/{languageCode}")
    public LanguageResponse update(
            @PathVariable String projectKey,
            @PathVariable String languageCode,
            @Valid @RequestBody UpdateLanguageRequest req
    ) {
        return service.update(projectKey, languageCode, req, actor());
    }

    @DeleteMapping("/{languageCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(@PathVariable String projectKey, @PathVariable String languageCode) {
        service.softDelete(projectKey, languageCode, actor());
    }

    @PostMapping("/{languageCode}/restore")
    public LanguageResponse restore(@PathVariable String projectKey, @PathVariable String languageCode) {
        return service.restore(projectKey, languageCode, actor());
    }
}

