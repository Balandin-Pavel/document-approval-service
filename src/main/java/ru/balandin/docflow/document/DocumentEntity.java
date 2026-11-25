package ru.balandin.docflow.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class DocumentEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false, length = 100)
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DocumentEntity() {}

    public DocumentEntity(String title, String body, String author, Instant now) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.body = body;
        this.author = author;
        this.status = DocumentStatus.DRAFT;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void submit(Instant now) {
        requireStatus(DocumentStatus.DRAFT);
        status = DocumentStatus.SUBMITTED;
        updatedAt = now;
    }

    public void decide(DocumentStatus decision, Instant now) {
        if (decision != DocumentStatus.APPROVED && decision != DocumentStatus.REJECTED) {
            throw new IllegalArgumentException("unsupported decision");
        }
        requireStatus(DocumentStatus.SUBMITTED);
        status = decision;
        updatedAt = now;
    }

    private void requireStatus(DocumentStatus expected) {
        if (status != expected) {
            throw new WorkflowConflictException("Expected " + expected + ", found " + status);
        }
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getAuthor() { return author; }
    public DocumentStatus getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
