package com.gem.evidencegraph.compliance;

import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.compliance.dto.CreateRequirementRequestDto;
import com.gem.evidencegraph.compliance.dto.RequirementResponseDto;

import java.util.List;
import java.util.UUID;

public interface ComplianceService {

    RequirementResponseDto createRequirement(UUID tenderId, CreateRequirementRequestDto request);

    List<RequirementResponseDto> getRequirementsByTender(UUID tenderId);

    RequirementResponseDto getRequirementById(UUID requirementId);

    BidComplianceResultDto evaluateBidCompliance(UUID bidId);

    BidComplianceResultDto getLatestBidCompliance(UUID bidId);

}
