package de.innologic.i18nservice.projectscope.service;

import de.innologic.i18nservice.projectscope.model.ProjectScopeEntity;
import de.innologic.i18nservice.projectscope.repo.ProjectScopeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectScopeService {

    private final ProjectScopeRepository repo;

    public ProjectScopeService(ProjectScopeRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ProjectScopeEntity create(String projectKey, String displayName) {
        ProjectKeyValidator.validate(projectKey);

        if (repo.existsByProjectKey(projectKey)) {
            throw new IllegalArgumentException("projectKey already exists: " + projectKey);
        }

        ProjectScopeEntity e = new ProjectScopeEntity();
        e.setProjectKey(projectKey);
        e.setDisplayName(displayName);

        return repo.save(e);
    }
}
