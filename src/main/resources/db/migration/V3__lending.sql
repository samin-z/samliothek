-- No FKs to copies/members: cross-module coupling stays in the application, not the schema.

CREATE TABLE loans (
    id              UUID PRIMARY KEY,
    copy_id         UUID        NOT NULL,
    member_id       UUID        NOT NULL,
    checked_out_at  TIMESTAMPTZ NOT NULL,
    due_on          DATE        NOT NULL,
    returned_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_loans_member_active ON loans(member_id) WHERE returned_at IS NULL;
CREATE UNIQUE INDEX idx_loans_copy_active ON loans(copy_id) WHERE returned_at IS NULL;
CREATE INDEX idx_loans_member_history ON loans(member_id, checked_out_at DESC);
