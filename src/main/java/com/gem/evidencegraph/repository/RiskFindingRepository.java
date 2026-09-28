package com.gem.evidencegraph.repository;

import com.gem.evidencegraph.entity.RiskFinding;
import com.gem.evidencegraph.risk.RiskDimension;
import com.gem.evidencegraph.risk.RiskSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface RiskFindingRepository extends JpaRepository<RiskFinding, UUID> {

    List<RiskFinding> findByRiskAssessmentId(UUID riskAssessmentId);

    List<RiskFinding> findByBidId(UUID bidId);

    List<RiskFinding> findByDimension(RiskDimension dimension);

    List<RiskFinding> findBySeverity(RiskSeverity severity);

    @Modifying
    @Transactional
    @Query("DELETE FROM RiskFinding rf WHERE rf.bid.id = :bidId")
    void deleteByBidId(@Param("bidId") UUID bidId);

}
