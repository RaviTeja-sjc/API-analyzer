-- Drop existing tables if re-running (safe for initial dev setup)
DROP TABLE IF EXISTS audit_events CASCADE;
DROP TABLE IF EXISTS patches CASCADE;
DROP TABLE IF EXISTS migration_suggestions CASCADE;
DROP TABLE IF EXISTS api_changes CASCADE;
DROP TABLE IF EXISTS reports CASCADE;
DROP TABLE IF EXISTS analysis_jobs CASCADE;
DROP TABLE IF EXISTS consumer_subscriptions CASCADE;
DROP TABLE IF EXISTS consumers CASCADE;
DROP TABLE IF EXISTS endpoints CASCADE;
DROP TABLE IF EXISTS source_files CASCADE;
DROP TABLE IF EXISTS api_versions CASCADE;
DROP TABLE IF EXISTS api_specs CASCADE;
DROP TABLE IF EXISTS project_members CASCADE;
DROP TABLE IF EXISTS projects CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Users & Roles
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);
INSERT INTO roles (name, description) VALUES ('ADMIN', 'Project Administrator'), ('MEMBER', 'Project Member'), ('VIEWER', 'Read-only access');

-- 2. Projects & Members
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    repository_url VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE project_members (
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles(id),
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, user_id)
);

-- 3. API Specifications & Versions
CREATE TABLE api_specs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    format VARCHAR(50) NOT NULL, -- e.g., OPENAPI, GRAPHQL
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (project_id, name)
);

CREATE TABLE api_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    api_spec_id UUID NOT NULL REFERENCES api_specs(id) ON DELETE CASCADE,
    version_tag VARCHAR(100) NOT NULL,
    status VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, DEPRECATED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (api_spec_id, version_tag)
);

-- 4. Source Files & Endpoints
CREATE TABLE source_files (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    api_version_id UUID NOT NULL REFERENCES api_versions(id) ON DELETE CASCADE,
    file_path VARCHAR(500) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    UNIQUE (api_version_id, file_path)
);

CREATE TABLE endpoints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    api_version_id UUID NOT NULL REFERENCES api_versions(id) ON DELETE CASCADE,
    method VARCHAR(10) NOT NULL,
    path VARCHAR(500) NOT NULL,
    operation_id VARCHAR(255),
    UNIQUE (api_version_id, method, path)
);

-- 5. Consumers
CREATE TABLE consumers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE consumer_subscriptions (
    consumer_id UUID NOT NULL REFERENCES consumers(id) ON DELETE CASCADE,
    api_spec_id UUID NOT NULL REFERENCES api_specs(id) ON DELETE CASCADE,
    PRIMARY KEY (consumer_id, api_spec_id)
);

-- 6. Analysis Jobs & Reports
CREATE TABLE analysis_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    base_version_id UUID NOT NULL REFERENCES api_versions(id) ON DELETE CASCADE,
    head_version_id UUID NOT NULL REFERENCES api_versions(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, RUNNING, COMPLETED, FAILED
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    analysis_job_id UUID NOT NULL REFERENCES analysis_jobs(id) ON DELETE CASCADE,
    is_breaking BOOLEAN NOT NULL DEFAULT false,
    summary_json JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. Changes, Suggestions, & Patches
CREATE TABLE api_changes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    endpoint_id UUID REFERENCES endpoints(id) ON DELETE SET NULL,
    change_type VARCHAR(100) NOT NULL, -- e.g., FIELD_REMOVED, TYPE_CHANGED
    severity VARCHAR(50) NOT NULL, -- NON_BREAKING, POTENTIALLY_BREAKING, BREAKING
    description TEXT NOT NULL
);

CREATE TABLE migration_suggestions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    api_change_id UUID NOT NULL REFERENCES api_changes(id) ON DELETE CASCADE,
    suggestion_text TEXT NOT NULL,
    code_snippet TEXT
);

CREATE TABLE patches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    language VARCHAR(50) NOT NULL, -- e.g., TYPESCRIPT, JAVA
    patch_content TEXT NOT NULL
);

-- 8. Audit Events
CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL, -- CREATED, UPDATED, DELETED, ANALYZED
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_project_members_user_id ON project_members(user_id);
CREATE INDEX idx_api_specs_project_id ON api_specs(project_id);
CREATE INDEX idx_api_versions_api_spec_id ON api_versions(api_spec_id);
CREATE INDEX idx_endpoints_api_version_id ON endpoints(api_version_id);
CREATE INDEX idx_analysis_jobs_base_head ON analysis_jobs(base_version_id, head_version_id);
CREATE INDEX idx_reports_analysis_job_id ON reports(analysis_job_id);
CREATE INDEX idx_api_changes_report_id ON api_changes(report_id);
CREATE INDEX idx_api_changes_endpoint_id ON api_changes(endpoint_id);
CREATE INDEX idx_patches_report_id ON patches(report_id);
CREATE INDEX idx_audit_events_entity ON audit_events(entity_type, entity_id);
CREATE INDEX idx_audit_events_user_id ON audit_events(user_id);

