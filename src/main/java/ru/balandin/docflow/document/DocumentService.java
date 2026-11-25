package ru.balandin.docflow.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.balandin.docflow.audit.DocumentEvent;
import ru.balandin.docflow.audit.DocumentEventRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {
    private final DocumentRepository documents;
    private final DocumentEventRepository events;
    private final Clock clock;

    public DocumentService(DocumentRepository documents, DocumentEventRepository events, Clock clock) {
        this.documents = documents;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public DocumentEntity create(String title, String body, String author) {
        Instant now = clock.instant();
        DocumentEntity document = documents.save(new DocumentEntity(title, body, author, now));
        events.save(new DocumentEvent(document.getId(), author, "CREATED", null, now));
        return document;
    }

    @Transactional
    public DocumentEntity submit(UUID id, String actor) {
        DocumentEntity document = find(id);
        if (!document.getAuthor().equals(actor)) {
            throw new SecurityException("Only the author can submit this document");
        }
        Instant now = clock.instant();
        document.submit(now);
        events.save(new DocumentEvent(id, actor, "SUBMITTED", null, now));
        return document;
    }

    @Transactional
    public DocumentEntity decide(UUID id, String actor, DocumentStatus decision, String comment) {
        DocumentEntity document = find(id);
        if (document.getAuthor().equals(actor)) {
            throw new SecurityException("The author cannot approve their own document");
        }
        if (decision == DocumentStatus.REJECTED && (comment == null || comment.isBlank())) {
            throw new IllegalArgumentException("A rejection requires a comment");
        }
        Instant now = clock.instant();
        document.decide(decision, now);
        events.save(new DocumentEvent(id, actor, decision.name(), comment, now));
        return document;
    }

    @Transactional(readOnly = true)
    public DocumentEntity get(UUID id, Authentication actor) {
        DocumentEntity document = find(id);
        requireVisible(document, actor);
        return document;
    }

    @Transactional(readOnly = true)
    public Page<DocumentEntity> list(Authentication actor, Pageable pageable) {
        if (isApprover(actor)) {
            return documents.findByStatusIn(
                    List.of(DocumentStatus.SUBMITTED, DocumentStatus.APPROVED, DocumentStatus.REJECTED),
                    pageable);
        }
        return documents.findByAuthor(actor.getName(), pageable);
    }

    @Transactional(readOnly = true)
    public List<DocumentEvent> events(UUID id, Authentication actor) {
        requireVisible(find(id), actor);
        return events.findByDocumentIdOrderByOccurredAtAsc(id);
    }

    private DocumentEntity find(UUID id) {
        return documents.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    private static void requireVisible(DocumentEntity document, Authentication actor) {
        if (document.getAuthor().equals(actor.getName())) return;
        if (isApprover(actor) && document.getStatus() != DocumentStatus.DRAFT) return;
        throw new SecurityException("Document is not visible to this user");
    }

    private static boolean isApprover(Authentication actor) {
        return actor.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_APPROVER"));
    }
}
