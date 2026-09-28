package com.gem.evidencegraph.repository;

import com.gem.evidencegraph.entity.ComplianceRequirement;
import com.gem.evidencegraph.entity.RequirementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplianceRequirementRepository extends JpaRepository<ComplianceRequirement, UUID> {

    List<ComplianceRequirement> findByTenderId(UUID tenderId);

    List<ComplianceRequirement> findByTenderIdAndMandatoryTrue(UUID tenderId);

    List<ComplianceRequirement> findByTenderIdAndRequirementType(UUID tenderId, RequirementType requirementType);

    Optional<ComplianceRequirement> findByTenderIdAndRequirementCode(UUID tenderId, String requirementCode);

}
