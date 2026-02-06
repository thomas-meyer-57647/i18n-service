-- Bundle Versioning:
-- - add fachliche bundle_version to current pointer table (1 row per language)
-- - introduce versions table (history)

ALTER TABLE i18n_language_bundle
    ADD COLUMN bundle_version INT NOT NULL DEFAULT 1 AFTER language_id;

CREATE TABLE i18n_language_bundle_version (
                                              id BIGINT NOT NULL AUTO_INCREMENT,

                                              language_id BIGINT NOT NULL,
                                              bundle_version INT NOT NULL,

                                              file_format VARCHAR(20) NOT NULL,
                                              original_file_name VARCHAR(255) NOT NULL,
                                              content_type VARCHAR(120) NULL,

                                              storage_path VARCHAR(500) NOT NULL,
                                              sha256 CHAR(64) NOT NULL,
                                              size_bytes BIGINT NOT NULL,

                                              uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                              uploaded_by VARCHAR(120) NULL,

                                              row_version BIGINT NOT NULL DEFAULT 0,

                                              PRIMARY KEY (id),

                                              CONSTRAINT uq_i18n_bundle_version_lang_ver UNIQUE (language_id, bundle_version),

                                              CONSTRAINT fk_i18n_bundle_version_language
                                                  FOREIGN KEY (language_id) REFERENCES i18n_language(id)
                                                      ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_i18n_bundle_version_language ON i18n_language_bundle_version (language_id);
CREATE INDEX idx_i18n_bundle_version_sha256 ON i18n_language_bundle_version (sha256);

-- Backfill: bestehende aktuelle Bundles werden zu Version 1
INSERT INTO i18n_language_bundle_version (
    language_id,
    bundle_version,
    file_format,
    original_file_name,
    content_type,
    storage_path,
    sha256,
    size_bytes,
    uploaded_at,
    uploaded_by,
    row_version
)
SELECT
    b.language_id,
    1 AS bundle_version,
    b.file_format,
    b.original_file_name,
    b.content_type,
    b.storage_path,
    b.sha256,
    b.size_bytes,
    b.uploaded_at,
    b.uploaded_by,
    0 AS row_version
FROM i18n_language_bundle b;
