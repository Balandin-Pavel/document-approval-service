package ru.balandin.docflow.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentEventRepository extends JpaRepository<DocumentEvent, UUID> {
    List<DocumentEvent> findByDocumentIdOrderByOccurredAtAsc(UUID documentId);
}
