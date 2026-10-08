ALTER TABLE vcs_connections RENAME COLUMN access_token TO encrypted_token;
ALTER TABLE vcs_connections DROP COLUMN webhook_secret;

ALTER TABLE vcs_connections ADD COLUMN default_branch VARCHAR(100);
ALTER TABLE vcs_connections ADD COLUMN spec_file_path VARCHAR(255);
ALTER TABLE vcs_connections ADD COLUMN source_directory VARCHAR(255);
