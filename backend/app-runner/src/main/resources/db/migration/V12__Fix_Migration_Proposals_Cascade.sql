-- Fix foreign key constraint on migration_proposals to cascade delete when project issues or analysis jobs are deleted
ALTER TABLE migration_proposals 
    DROP CONSTRAINT IF EXISTS fk_migration_proposals_project_issue;

ALTER TABLE migration_proposals 
    ADD CONSTRAINT fk_migration_proposals_project_issue 
    FOREIGN KEY (project_issue_id) 
    REFERENCES project_issues(id) 
    ON DELETE CASCADE;

ALTER TABLE migration_proposals 
    DROP CONSTRAINT IF EXISTS fk_migration_proposals_analysis_job;

ALTER TABLE migration_proposals 
    ADD CONSTRAINT fk_migration_proposals_analysis_job 
    FOREIGN KEY (analysis_job_id) 
    REFERENCES analysis_jobs(id) 
    ON DELETE CASCADE;
