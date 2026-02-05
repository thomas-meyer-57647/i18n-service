package de.innologic.i18nservice.projectscope.controller;

import de.innologic.i18nservice.projectscope.model.ProjectScopeEntity;
import de.innologic.i18nservice.projectscope.service.ProjectScopeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * POST /api/v1/projects → legt ein Projekt an
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectScopeController {

    private final ProjectScopeService service;

    public ProjectScopeController(ProjectScopeService service) {
        this.service = service;
    }

    public record CreateProjectRequest(String projectKey, String displayName) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectScopeEntity create(@RequestBody CreateProjectRequest req) {
        return service.create(req.projectKey(), req.displayName());
    }
}
