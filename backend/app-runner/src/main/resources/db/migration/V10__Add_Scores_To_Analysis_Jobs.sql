-- Add scores and score breakdown to analysis_jobs
ALTER TABLE analysis_jobs ADD COLUMN security_score INT;
ALTER TABLE analysis_jobs ADD COLUMN api_health_score INT;
ALTER TABLE analysis_jobs ADD COLUMN score_breakdown TEXT;
