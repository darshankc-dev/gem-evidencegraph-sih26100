package com.gem.evidencegraph.repository;

import com.gem.evidencegraph.entity.EvidenceRelationship;
import com.gem.evidencegraph.entity.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceRelationshipRepository extends JpaRepository<EvidenceRelationship, UUID> {

    List<EvidenceRelationship> findBySourceEvidenceId(UUID sourceEvidenceId);

    List<EvidenceRelationship> findBySourceClaimId(UUID sourceClaimId);

    List<EvidenceRelationship> findByTargetEvidenceId(UUID targetEvidenceId);

    List<EvidenceRelationship> findByRelationshipType(RelationshipType relationshipType);

    List<EvidenceRelationship> findBySourceEvidenceIdAndRelationshipType(UUID sourceEvidenceId, RelationshipType relationshipType);

    java.util.Optional<EvidenceRelationship> findBySourceClaimIdAndTargetEvidenceIdAndRelationshipType(
            UUID sourceClaimId,
            UUID targetEvidenceId,
            RelationshipType relationshipType
    );

}
