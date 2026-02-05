ALTER TABLE i18n_project_scope
    ADD COLUMN default_language_code VARCHAR(35) NULL,
  ADD COLUMN fallback_language_code VARCHAR(35) NULL,
  ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_i18n_project_scope_default_lang ON i18n_project_scope (default_language_code);
CREATE INDEX idx_i18n_project_scope_fallback_lang ON i18n_project_scope (fallback_language_code);
