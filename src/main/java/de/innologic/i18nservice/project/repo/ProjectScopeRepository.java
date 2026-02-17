package de.innologic.i18nservice.project.repo;

import de.innologic.i18nservice.project.model.ProjectScopeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectScopeRepository extends JpaRepository<ProjectScopeEntity, Long> {
    boolean existsByProjectKey(String projectKey);
    Optional<ProjectScopeEntity> findByProjectKey(String projectKey);
    List<ProjectScopeEntity> findByProjectKeyOrderByProjectKeyAsc(String projectKey);
}
