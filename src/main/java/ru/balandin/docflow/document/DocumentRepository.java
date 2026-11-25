package ru.balandin.docflow.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {
    Page<DocumentEntity> findByAuthor(String author, Pageable pageable);
    Page<DocumentEntity> findByStatusIn(Collection<DocumentStatus> statuses, Pageable pageable);
}
