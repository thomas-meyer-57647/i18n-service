package de.innologic.i18nservice.projectscope.repo;

import de.innologic.i18nservice.projectscope.model.ProjectScopeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectScopeRepository extends JpaRepository<ProjectScopeEntity, Long> {
    boolean existsByProjectKey(String projectKey);
    Optional<ProjectScopeEntity> findByProjectKey(String projectKey);
}
