-- Optimistic locking / insert detection for client-assigned UUIDs (Spring Data JDBC).

ALTER TABLE members ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
