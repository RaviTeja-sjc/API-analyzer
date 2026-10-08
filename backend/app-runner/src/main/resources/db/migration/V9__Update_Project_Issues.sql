ALTER TABLE project_issues ADD COLUMN rule_id VARCHAR(255);
UPDATE project_issues SET rule_id = 'UNKNOWN' WHERE rule_id IS NULL;
ALTER TABLE project_issues ALTER COLUMN rule_id SET NOT NULL;

ALTER TABLE project_issues ADD COLUMN start_line INTEGER;
ALTER TABLE project_issues ADD COLUMN end_line INTEGER;
ALTER TABLE project_issues ADD COLUMN metadata TEXT;

ALTER TABLE project_issues DROP COLUMN line_range;
