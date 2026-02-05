CREATE TABLE i18n_language (
                          id BIGINT NOT NULL AUTO_INCREMENT,

                          project_key VARCHAR(32) NOT NULL,
                          language_code VARCHAR(35) NOT NULL,  -- BCP-47 (z.B. de-DE, zh-Hant-TW)
                          name VARCHAR(120) NOT NULL,

                          is_active TINYINT(1) NOT NULL DEFAULT 1,

    -- Soft delete
                          is_deleted TINYINT(1) NOT NULL DEFAULT 0,
                          deleted_at TIMESTAMP NULL,
                          deleted_by VARCHAR(120) NULL,

    -- Audit fields (minimal)
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          created_by VARCHAR(120) NULL,
                          modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          modified_by VARCHAR(120) NULL,

    -- Optimistic locking
                          row_version BIGINT NOT NULL DEFAULT 0,

                          PRIMARY KEY (id),

    -- Eindeutigkeit pro Projekt: nur eine "aktive" (is_deleted=0) pro Code
                          CONSTRAINT uq_language_project_code_deleted UNIQUE (project_key, language_code, is_deleted),

                          CONSTRAINT fk_language_project_key
                              FOREIGN KEY (project_key) REFERENCES i18n_project_scope(project_key)
                                  ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_i18n_language_project ON i18n_language (project_key);
CREATE INDEX idx_i18n_language_project_active ON i18n_language (project_key, is_active);
