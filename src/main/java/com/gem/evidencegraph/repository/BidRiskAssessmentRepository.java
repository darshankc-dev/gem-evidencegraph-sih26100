package com.gem.evidencegraph.repository;

import com.gem.evidencegraph.entity.BidRiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BidRiskAssessmentRepository extends JpaRepository<BidRiskAssessment, UUID> {

    Optional<BidRiskAssessment> findFirstByBidIdOrderByEvaluatedAtDesc(UUID bidId);

    Optional<BidRiskAssessment> findByBidId(UUID bidId);

    List<BidRiskAssessment> findByBidIdOrderByEvaluatedAtDesc(UUID bidId);

    @Modifying
    @Transactional
    @Query("DELETE FROM BidRiskAssessment bra WHERE bra.bid.id = :bidId")
    void deleteByBidId(@Param("bidId") UUID bidId);

}
