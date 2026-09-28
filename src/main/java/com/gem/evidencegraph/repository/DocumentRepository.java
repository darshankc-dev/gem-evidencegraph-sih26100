package com.gem.evidencegraph.repository;

import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findBySha256Hash(String sha256Hash);

    List<Document> findByBidId(UUID bidId);

    List<Document> findByBidIdAndDocumentType(UUID bidId, DocumentType documentType);

}
