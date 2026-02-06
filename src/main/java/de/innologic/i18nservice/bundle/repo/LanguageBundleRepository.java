package de.innologic.i18nservice.bundle.repo;

import de.innologic.i18nservice.bundle.model.LanguageBundleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LanguageBundleRepository extends JpaRepository<LanguageBundleEntity, Long> {
    Optional<LanguageBundleEntity> findByLanguageId(Long languageId);
}
