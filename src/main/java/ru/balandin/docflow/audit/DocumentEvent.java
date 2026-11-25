package ru.balandin.docflow.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_events")
public class DocumentEvent {
    @Id
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(nullable = false, length = 100)
    private String actor;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected DocumentEvent() {}

    public DocumentEvent(UUID documentId, String actor, String action, String comment, Instant occurredAt) {
        this.id = UUID.randomUUID();
        this.documentId = documentId;
        this.actor = actor;
        this.action = action;
        this.comment = comment;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getComment() { return comment; }
    public Instant getOccurredAt() { return occurredAt; }
}
