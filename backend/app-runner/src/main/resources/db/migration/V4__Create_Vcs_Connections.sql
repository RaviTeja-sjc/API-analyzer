CREATE TABLE vcs_connections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider VARCHAR(50) NOT NULL,
    repository_url VARCHAR(255) NOT NULL,
    access_token VARCHAR(255),
    webhook_secret VARCHAR(255),
    is_validated BOOLEAN DEFAULT FALSE,
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
