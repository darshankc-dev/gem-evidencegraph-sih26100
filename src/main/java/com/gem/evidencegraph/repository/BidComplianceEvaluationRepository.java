package com.gem.evidencegraph.repository;

import com.gem.evidencegraph.entity.BidComplianceEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BidComplianceEvaluationRepository extends JpaRepository<BidComplianceEvaluation, UUID> {

    List<BidComplianceEvaluation> findByBidIdOrderByCreatedAtDesc(UUID bidId);

    Optional<BidComplianceEvaluation> findFirstByBidIdOrderByCreatedAtDesc(UUID bidId);

}
