-- Catalog aggregate tables. Cross-module FKs intentionally omitted (see PROJECT.md §6 / §13).

CREATE TABLE books (
    id             UUID PRIMARY KEY,
    isbn           VARCHAR(13)  NOT NULL UNIQUE,
    title          VARCHAR(500) NOT NULL,
    author         VARCHAR(300) NOT NULL,
    published_year INT          NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE copies (
    id         UUID PRIMARY KEY,
    book_id    UUID        NOT NULL REFERENCES books(id),
    barcode    VARCHAR(50) NOT NULL UNIQUE,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_copies_book_id ON copies(book_id);
CREATE INDEX idx_copies_status  ON copies(status);
