package de.innologic.i18nservice.project.controller;

import de.innologic.i18nservice.project.dto.CreateProjectRequest;
import de.innologic.i18nservice.project.dto.ProjectScopeResponse;
import de.innologic.i18nservice.project.service.ProjectScopeService;
import de.innologic.i18nservice.security.JwtPrincipalAccessor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectScopeController {

    private final ProjectScopeService service;
    private final JwtPrincipalAccessor principalAccessor;

    public ProjectScopeController(ProjectScopeService service, JwtPrincipalAccessor principalAccessor) {
        this.service = service;
        this.principalAccessor = principalAccessor;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectScopeResponse create(@Valid @RequestBody CreateProjectRequest req) {
        return service.create(req, principalAccessor.currentSubject(), principalAccessor.currentTenantId());
    }

    // Optional, aber sehr praktisch (Admin/API-Nutzung):
    @GetMapping
    public List<ProjectScopeResponse> list() {
        return service.list(principalAccessor.currentTenantId());
    }

    // Optional:
    @GetMapping("/{projectKey}")
    public ProjectScopeResponse get(@PathVariable String projectKey) {
        return service.get(projectKey);
    }
}
