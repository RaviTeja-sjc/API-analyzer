CREATE TABLE project_issues (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    analysis_job_id UUID REFERENCES analysis_jobs(id) ON DELETE CASCADE,
    category VARCHAR(255) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    confidence VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    problem_description TEXT NOT NULL,
    impact TEXT,
    recommendation TEXT,
    file_path VARCHAR(1000),
    line_range VARCHAR(100),
    evidence TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_project_issues_project_id ON project_issues(project_id);
CREATE INDEX idx_project_issues_analysis_job_id ON project_issues(analysis_job_id);
