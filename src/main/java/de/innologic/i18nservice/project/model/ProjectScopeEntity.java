package de.innologic.i18nservice.project.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Entity
@Table(
        name = "i18n_project_scope",
        uniqueConstraints = @UniqueConstraint(name = "uq_project_scope_key", columnNames = "project_key")
)
public class ProjectScopeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_key", nullable = false, length = 32)
    private String projectKey;

    @Column(name = "display_name", length = 120)
    private String displayName;

    // P0-3: Default/Fallback Sprache pro Projekt
    @Column(name = "default_language_code", length = 35)
    private String defaultLanguageCode;

    @Column(name = "fallback_language_code", length = 35)
    private String fallbackLanguageCode;

    // P0-3: Optimistic Locking
    @Version
    @Column(name = "row_version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.modifiedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.modifiedAt = Instant.now();
    }
}
