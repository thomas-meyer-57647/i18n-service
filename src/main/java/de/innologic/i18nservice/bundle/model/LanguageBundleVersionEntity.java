package de.innologic.i18nservice.bundle.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "i18n_language_bundle_version",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_i18n_bundle_version_lang_ver",
                        columnNames = {"language_id", "bundle_version"}
                )
        },
        indexes = {
                @Index(name = "idx_i18n_bundle_version_language", columnList = "language_id"),
                @Index(name = "idx_i18n_bundle_version_sha256", columnList = "sha256")
        }
)
public class LanguageBundleVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "language_id", nullable = false)
    private Long languageId;

    /**
     * Fachliche Bundle-Version (1..n).
     */
    @Column(name = "bundle_version", nullable = false)
    private int bundleVersion;

    @Column(name = "file_format", nullable = false, length = 20)
    private String fileFormat;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt = Instant.now();

    @Column(name = "uploaded_by", length = 120)
    private String uploadedBy;

    @Version
    @Column(name = "row_version", nullable = false)
    private long version;
}
