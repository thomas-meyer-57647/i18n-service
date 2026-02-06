CREATE TABLE i18n_audit_log (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                occurred_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

                                project_key VARCHAR(32) NOT NULL,
                                actor VARCHAR(120) NULL,

                                action VARCHAR(64) NOT NULL,
                                entity_type VARCHAR(32) NOT NULL,
                                entity_key VARCHAR(255) NOT NULL,

                                details_json LONGTEXT NULL,
                                request_id VARCHAR(64) NULL,

                                PRIMARY KEY (id),
                                INDEX idx_audit_project_time (project_key, occurred_at),
                                INDEX idx_audit_entity (project_key, entity_type, entity_key, occurred_at)
) ENGINE=InnoDB;
