package com.gem.evidencegraph.repository;

import com.gem.evidencegraph.entity.TemporalEvaluation;
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
public interface TemporalEvaluationRepository extends JpaRepository<TemporalEvaluation, UUID> {

    List<TemporalEvaluation> findByBidId(UUID bidId);

    List<TemporalEvaluation> findByEvidenceId(UUID evidenceId);

    List<TemporalEvaluation> findByBidIdOrderByEvaluatedAtDesc(UUID bidId);

    List<TemporalEvaluation> findByEvidenceIdOrderByEvaluatedAtDesc(UUID evidenceId);

    Optional<TemporalEvaluation> findFirstByBidIdAndEvidenceIdOrderByEvaluatedAtDesc(UUID bidId, UUID evidenceId);

    Optional<TemporalEvaluation> findByBidIdAndEvidenceId(UUID bidId, UUID evidenceId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TemporalEvaluation te WHERE te.bid.id = :bidId")
    void deleteByBidId(@Param("bidId") UUID bidId);

}
