package com.gem.evidencegraph.risk;

import com.gem.evidencegraph.risk.dto.BidRiskAssessmentResponseDto;
import com.gem.evidencegraph.risk.dto.RiskDimensionResultDto;

import java.util.List;
import java.util.UUID;

public interface RiskService {

    BidRiskAssessmentResponseDto evaluateBidRisk(UUID bidId);

    BidRiskAssessmentResponseDto getBidRisk(UUID bidId);

    List<RiskDimensionResultDto> getBidRiskDimensions(UUID bidId);

}
