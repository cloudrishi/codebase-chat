-- codebase-chat database initialization
-- Runs once on first container start via /docker-entrypoint-initdb.d/

CREATE EXTENSION IF NOT EXISTS vector;

CREATE SCHEMA IF NOT EXISTS codebase;

CREATE TABLE IF NOT EXISTS codebase.code_chunks (
    id          BIGSERIAL PRIMARY KEY,
    file_path   TEXT        NOT NULL,
    class_name  TEXT,
    method_name TEXT,
    chunk_type  TEXT        NOT NULL,
    content     TEXT        NOT NULL,
    embedding   vector(384) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Cosine similarity index for pgvector ANN search
CREATE INDEX IF NOT EXISTS code_chunks_embedding_idx
    ON codebase.code_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- Speeds up the scoped DELETE on re-index
CREATE INDEX IF NOT EXISTS code_chunks_file_path_idx
    ON codebase.code_chunks (file_path);
