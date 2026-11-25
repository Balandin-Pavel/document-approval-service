CREATE TABLE documents (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    author VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE document_events (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id),
    actor VARCHAR(100) NOT NULL,
    action VARCHAR(30) NOT NULL,
    comment TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_documents_author ON documents(author);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_document_events_document_time ON document_events(document_id, occurred_at);
