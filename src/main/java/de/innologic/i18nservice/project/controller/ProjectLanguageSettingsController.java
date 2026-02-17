package de.innologic.i18nservice.project.controller;

import de.innologic.i18nservice.common.context.RequestContext;
import de.innologic.i18nservice.project.dto.LanguageSettingsResponse;
import de.innologic.i18nservice.project.dto.UpdateLanguageSettingsRequest;
import de.innologic.i18nservice.project.service.ProjectLanguageSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/{projectKey}/language-settings")
public class ProjectLanguageSettingsController {

    private final ProjectLanguageSettingsService service;

    public ProjectLanguageSettingsController(ProjectLanguageSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public LanguageSettingsResponse get(@PathVariable String projectKey) {
        return service.get(projectKey);
    }

    @PutMapping
    public LanguageSettingsResponse update(
            @PathVariable String projectKey,
            @Valid @RequestBody UpdateLanguageSettingsRequest req
    ) {
        return service.update(projectKey, req, RequestContext.actor());
    }
}
