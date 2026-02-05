package de.innologic.i18nservice.language.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "i18n_language",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_language_project_code_deleted",
                columnNames = {"project_key", "language_code", "is_deleted"}
        )
)
public class LanguageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "project_key", nullable = false, length = 32)
    private String projectKey;

    @Column(name = "language_code", nullable = false, length = 35)
    private String languageCode;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 120)
    private String deletedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt = Instant.now();

    @Column(name = "modified_by", length = 120)
    private String modifiedBy;

    @Version
    @Column(name = "row_version", nullable = false)
    private long version;

    @PreUpdate
    void onUpdate() {
        this.modifiedAt = Instant.now();
    }
}
