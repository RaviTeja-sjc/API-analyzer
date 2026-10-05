UPDATE roles SET name = 'PROJECT_OWNER' WHERE name = 'ADMIN';
UPDATE roles SET name = 'DEVELOPER' WHERE name = 'MEMBER';
INSERT INTO roles (name, description) VALUES ('GLOBAL_ADMIN', 'System Administrator');
