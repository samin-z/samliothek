-- Optimistic locking / insert detection for client-assigned UUIDs (Spring Data JDBC).

ALTER TABLE books ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE copies ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
