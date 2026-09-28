package com.gem.evidencegraph.repository;

import com.gem.evidencegraph.entity.RiskAssessment;
import com.gem.evidencegraph.risk.RiskDimension;
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
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    List<RiskAssessment> findByBidId(UUID bidId);

    List<RiskAssessment> findByBidRiskAssessmentId(UUID bidRiskAssessmentId);

    Optional<RiskAssessment> findByBidRiskAssessmentIdAndDimension(UUID bidRiskAssessmentId, RiskDimension dimension);

    @Modifying
    @Transactional
    @Query("DELETE FROM RiskAssessment ra WHERE ra.bid.id = :bidId")
    void deleteByBidId(@Param("bidId") UUID bidId);

}
