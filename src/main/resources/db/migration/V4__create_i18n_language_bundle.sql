CREATE TABLE i18n_language_bundle (
                                      id BIGINT NOT NULL AUTO_INCREMENT,

                                      language_id BIGINT NOT NULL,

                                      file_format VARCHAR(20) NOT NULL,        -- e.g. JSON
                                      original_file_name VARCHAR(255) NOT NULL,
                                      content_type VARCHAR(120) NULL,

                                      storage_path VARCHAR(500) NOT NULL,
                                      sha256 CHAR(64) NOT NULL,
                                      size_bytes BIGINT NOT NULL,

                                      uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      uploaded_by VARCHAR(120) NULL,

                                      row_version BIGINT NOT NULL DEFAULT 0,

                                      PRIMARY KEY (id),

                                      CONSTRAINT uq_i18n_bundle_language UNIQUE (language_id),

                                      CONSTRAINT fk_i18n_bundle_language
                                          FOREIGN KEY (language_id) REFERENCES i18n_language(id)
                                              ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_i18n_bundle_sha256 ON i18n_language_bundle (sha256);
