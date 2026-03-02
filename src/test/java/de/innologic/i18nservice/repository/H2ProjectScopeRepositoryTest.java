package de.innologic.i18nservice.repository;

import de.innologic.i18nservice.project.model.ProjectScopeEntity;
import de.innologic.i18nservice.project.repo.ProjectScopeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("h2")
class H2ProjectScopeRepositoryTest {

    @Autowired
    ProjectScopeRepository repository;

    @Test
    void savesAndReadsProjectScope() {
        var entity = new ProjectScopeEntity();
        entity.setProjectKey("demo");
        entity.setDisplayName("Demo Project");
        entity.setDefaultLanguageCode("en-US");
        entity.setFallbackLanguageCode("en-US");

        var saved = repository.saveAndFlush(entity);

        assertThat(saved.getId()).isNotNull();
        var fetched = repository.findByProjectKey("demo").orElseThrow();
        assertThat(fetched.getProjectKey()).isEqualTo("demo");
        assertThat(fetched.getCreatedAt()).isNotNull();
    }

    @Test
    void missingProjectKeyTriggersConstraintViolation() {
        var entity = new ProjectScopeEntity();
        entity.setDisplayName("No key set");

        assertThatThrownBy(() -> repository.saveAndFlush(entity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
