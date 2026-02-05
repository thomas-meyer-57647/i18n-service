CREATE TABLE project_scope (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               project_key VARCHAR(32) NOT NULL,
                               display_name VARCHAR(120) NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               PRIMARY KEY (id),
                               CONSTRAINT uq_project_scope_key UNIQUE (project_key)
);
