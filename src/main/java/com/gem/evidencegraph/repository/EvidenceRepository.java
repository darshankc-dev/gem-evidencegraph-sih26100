package com.gem.evidencegraph.repository;

import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {

    List<Evidence> findByBidId(UUID bidId);

    List<Evidence> findByClaimId(UUID claimId);

    List<Evidence> findByVerificationStatus(VerificationStatus verificationStatus);

    List<Evidence> findBySourceType(SourceType sourceType);

    List<Evidence> findBySubjectAndAttribute(String subject, String attribute);

    java.util.Optional<Evidence> findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(
            UUID claimId,
            SourceType sourceType,
            String sourceReference,
            String value,
            VerificationStatus verificationStatus
    );

}
