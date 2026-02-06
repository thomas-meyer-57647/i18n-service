package de.innologic.i18nservice.project.controller;

import de.innologic.i18nservice.project.dto.CreateProjectRequest;
import de.innologic.i18nservice.project.dto.ProjectScopeResponse;
import de.innologic.i18nservice.project.service.ProjectScopeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectScopeController {

    private final ProjectScopeService service;

    public ProjectScopeController(ProjectScopeService service) {
        this.service = service;
    }

    /**
     * Falls du später IAM/JWT nutzt, ziehst du den Actor aus dem SecurityContext.
     * Für P0 reicht ein Platzhalter.
     */
    private String actor() {
        return "system";
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectScopeResponse create(@Valid @RequestBody CreateProjectRequest req) {
        return service.create(req, actor());
    }

    // Optional, aber sehr praktisch (Admin/API-Nutzung):
    @GetMapping
    public List<ProjectScopeResponse> list() {
        return service.list();
    }

    // Optional:
    @GetMapping("/{projectKey}")
    public ProjectScopeResponse get(@PathVariable String projectKey) {
        return service.get(projectKey);
    }
}
