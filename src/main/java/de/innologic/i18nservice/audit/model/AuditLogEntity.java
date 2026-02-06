package de.innologic.i18nservice.audit.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "i18n_audit_log",
        indexes = {
                @Index(name = "idx_audit_project_time", columnList = "project_key, occurred_at"),
                @Index(name = "idx_audit_entity", columnList = "project_key, entity_type, entity_key, occurred_at")
        }
)
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "project_key", nullable = false, length = 32)
    private String projectKey;

    @Column(name = "actor", length = 120)
    private String actor;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 32)
    private String entityType;

    @Column(name = "entity_key", nullable = false, length = 255)
    private String entityKey;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "details_json", columnDefinition = "LONGTEXT")
    private String detailsJson;

    @Column(name = "request_id", length = 64)
    private String requestId;
}
