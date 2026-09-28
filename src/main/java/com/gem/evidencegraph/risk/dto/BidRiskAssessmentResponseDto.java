package com.gem.evidencegraph.risk.dto;

import com.gem.evidencegraph.risk.DecisionRecommendation;
import com.gem.evidencegraph.risk.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidRiskAssessmentResponseDto {

    private UUID bidId;
    private RiskLevel overallRiskLevel;
    private int overallRiskScore;
    private DecisionRecommendation decisionRecommendation;
    private boolean hardFail;
    private String summary;
    private String reason;

    @Builder.Default
    private List<RiskDimensionResultDto> dimensions = new ArrayList<>();

    private LocalDateTime evaluatedAt;

}
