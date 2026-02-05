package de.innologic.i18nservice.language.repo;

import de.innologic.i18nservice.language.model.LanguageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LanguageRepository extends JpaRepository<LanguageEntity, Long> {

    List<LanguageEntity> findByProjectKeyAndDeletedFalseOrderByLanguageCodeAsc(String projectKey);

    Optional<LanguageEntity> findByProjectKeyAndLanguageCodeAndDeletedFalse(String projectKey, String languageCode);

    Optional<LanguageEntity> findByProjectKeyAndLanguageCodeAndDeletedTrue(String projectKey, String languageCode);

    boolean existsByProjectKeyAndLanguageCodeAndDeletedFalse(String projectKey, String languageCode);
}
