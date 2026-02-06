package de.innologic.i18nservice.audit.repo;

import de.innologic.i18nservice.audit.model.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    Page<AuditLogEntity> findByProjectKeyOrderByOccurredAtDesc(String projectKey, Pageable pageable);

    Page<AuditLogEntity> findByProjectKeyAndEntityTypeAndEntityKeyOrderByOccurredAtDesc(
            String projectKey, String entityType, String entityKey, Pageable pageable
    );
}
