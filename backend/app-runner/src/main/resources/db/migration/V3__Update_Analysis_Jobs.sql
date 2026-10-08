-- Update analysis_jobs to match the Java entity schema
ALTER TABLE analysis_jobs DROP COLUMN base_version_id;
ALTER TABLE analysis_jobs DROP COLUMN head_version_id;
ALTER TABLE analysis_jobs DROP COLUMN started_at;
ALTER TABLE analysis_jobs DROP COLUMN completed_at;

ALTER TABLE analysis_jobs ADD COLUMN project_id UUID;
ALTER TABLE analysis_jobs ADD COLUMN progress INT DEFAULT 0;
ALTER TABLE analysis_jobs ADD COLUMN logs TEXT;
ALTER TABLE analysis_jobs ADD COLUMN idempotency_key VARCHAR(255);
ALTER TABLE analysis_jobs ADD COLUMN report_id UUID;
ALTER TABLE analysis_jobs ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
