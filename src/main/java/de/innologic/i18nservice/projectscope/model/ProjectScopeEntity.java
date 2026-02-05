package de.innologic.i18nservice.projectscope.model;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "project_scope",
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.modifiedAt = Instant.now();
    }

    // Getter/Setter …
}
