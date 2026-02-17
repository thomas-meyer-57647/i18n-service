package de.innologic.i18nservice.language.controller;

import de.innologic.i18nservice.common.context.RequestContext;
import de.innologic.i18nservice.language.dto.CreateLanguageRequest;
import de.innologic.i18nservice.language.dto.LanguageResponse;
import de.innologic.i18nservice.language.dto.UpdateLanguageRequest;
import de.innologic.i18nservice.language.service.LanguageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/{projectKey}/languages")
public class LanguageController {

    private final LanguageService service;

    public LanguageController(LanguageService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LanguageResponse create(
            @PathVariable String projectKey,
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
    public List<LanguageResponse> list(
            @PathVariable String projectKey,
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
    public LanguageResponse get(
            @PathVariable String projectKey,
            @PathVariable String languageCode
    ) {
        return service.get(projectKey, languageCode);
    }

    @PutMapping("/{languageCode}")
    public LanguageResponse update(
            @PathVariable String projectKey,
            @PathVariable String languageCode,
            @Valid @RequestBody UpdateLanguageRequest req
    ) {
        return service.update(projectKey, languageCode, req, RequestContext.actor());
    }

    /**
     * Soft delete -> Papierkorb
     */
    @DeleteMapping("/{languageCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(
            @PathVariable String projectKey,
            @PathVariable String languageCode
    ) {
        service.softDelete(projectKey, languageCode, RequestContext.actor());
    }

    @PostMapping("/{languageCode}/restore")
    public LanguageResponse restore(
            @PathVariable String projectKey,
            @PathVariable String languageCode
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
    public void purge(
            @PathVariable String projectKey,
            @PathVariable String languageCode,
            @RequestParam(name = "force", defaultValue = "false") boolean force
    ) {
        service.purge(projectKey, languageCode, force, RequestContext.actor());
    }
}
