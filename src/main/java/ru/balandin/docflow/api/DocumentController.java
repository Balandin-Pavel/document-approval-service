package ru.balandin.docflow.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.balandin.docflow.audit.DocumentEvent;
import ru.balandin.docflow.document.DocumentEntity;
import ru.balandin.docflow.document.DocumentService;
import ru.balandin.docflow.document.DocumentStatus;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    public record CreateRequest(@NotBlank @Size(max = 200) String title,
                                @NotBlank @Size(max = 5000) String body) {}
    public record DecisionRequest(@Size(max = 1000) String comment) {}
    public record DocumentResponse(UUID id, String title, String body, String author,
                                   DocumentStatus status, long version,
                                   Instant createdAt, Instant updatedAt) {
        static DocumentResponse from(DocumentEntity document) {
            return new DocumentResponse(document.getId(), document.getTitle(), document.getBody(),
                    document.getAuthor(), document.getStatus(), document.getVersion(),
                    document.getCreatedAt(), document.getUpdatedAt());
        }
    }
    public record EventResponse(String actor, String action, String comment, Instant occurredAt) {
        static EventResponse from(DocumentEvent event) {
            return new EventResponse(event.getActor(), event.getAction(),
                    event.getComment(), event.getOccurredAt());
        }
    }
    public record PageResponse(List<DocumentResponse> items, long total, int page, int size) {}

    @PostMapping
    @PreAuthorize("hasRole('AUTHOR')")
    public ResponseEntity<DocumentResponse> create(@Valid @RequestBody CreateRequest request,
                                                    Authentication actor) {
        DocumentResponse document = DocumentResponse.from(
                service.create(request.title(), request.body(), actor.getName()));
        return ResponseEntity.created(URI.create("/api/documents/" + document.id())).body(document);
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable UUID id, Authentication actor) {
        return DocumentResponse.from(service.get(id, actor));
    }

    @GetMapping
    public PageResponse list(Authentication actor,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be >= 0 and size must be 1..100");
        }
        Page<DocumentResponse> documents = service.list(actor,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(DocumentResponse::from);
        return new PageResponse(documents.getContent(), documents.getTotalElements(), page, size);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('AUTHOR')")
    public DocumentResponse submit(@PathVariable UUID id, Authentication actor) {
        return DocumentResponse.from(service.submit(id, actor.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('APPROVER')")
    public DocumentResponse approve(@PathVariable UUID id,
                                    @RequestBody(required = false) DecisionRequest request,
                                    Authentication actor) {
        String comment = request == null ? null : request.comment();
        return DocumentResponse.from(service.decide(id, actor.getName(), DocumentStatus.APPROVED, comment));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('APPROVER')")
    public DocumentResponse reject(@PathVariable UUID id,
                                   @Valid @RequestBody DecisionRequest request,
                                   Authentication actor) {
        return DocumentResponse.from(service.decide(id, actor.getName(),
                DocumentStatus.REJECTED, request.comment()));
    }

    @GetMapping("/{id}/events")
    public List<EventResponse> events(@PathVariable UUID id, Authentication actor) {
        return service.events(id, actor).stream().map(EventResponse::from).toList();
    }
}
