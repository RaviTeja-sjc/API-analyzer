ALTER TABLE analysis_jobs ADD COLUMN total_files_discovered INT;
ALTER TABLE analysis_jobs ADD COLUMN files_analyzed INT;
ALTER TABLE analysis_jobs ADD COLUMN files_skipped INT;
ALTER TABLE analysis_jobs ADD COLUMN unsupported_files INT;
ALTER TABLE analysis_jobs ADD COLUMN languages TEXT;
