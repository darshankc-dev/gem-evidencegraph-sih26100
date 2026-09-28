package com.gem.evidencegraph.repository;

import com.gem.evidencegraph.entity.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    List<Claim> findByDocumentId(UUID documentId);

    List<Claim> findByFieldName(String fieldName);

    List<Claim> findByDocumentBidId(UUID bidId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Claim c WHERE c.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);

}
