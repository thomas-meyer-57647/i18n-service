package de.innologic.i18nservice.bundle.repo;

import de.innologic.i18nservice.bundle.model.LanguageBundleVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LanguageBundleVersionRepository extends JpaRepository<LanguageBundleVersionEntity, Long> {

    List<LanguageBundleVersionEntity> findByLanguageIdOrderByBundleVersionDesc(Long languageId);

    Optional<LanguageBundleVersionEntity> findTopByLanguageIdOrderByBundleVersionDesc(Long languageId);

    Optional<LanguageBundleVersionEntity> findByLanguageIdAndBundleVersion(Long languageId, int bundleVersion);
}
