package com.gem.evidencegraph.repository;

import com.gem.evidencegraph.entity.EntityResolutionResult;
import com.gem.evidencegraph.entityresolution.EntityMatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EntityResolutionResultRepository extends JpaRepository<EntityResolutionResult, UUID> {

    List<EntityResolutionResult> findByBidderId(UUID bidderId);

    List<EntityResolutionResult> findByMatchStatus(EntityMatchStatus matchStatus);

    List<EntityResolutionResult> findByBidderIdOrderByCreatedAtDesc(UUID bidderId);

}
