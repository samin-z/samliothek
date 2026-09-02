CREATE TABLE members (
    id         UUID PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    email      VARCHAR(320) NOT NULL UNIQUE,
    tier       VARCHAR(20)  NOT NULL,
    joined_on  DATE         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
