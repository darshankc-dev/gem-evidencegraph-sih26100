package com.gem.evidencegraph.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoBidSummaryDto {

    private UUID bidId;
    private String bidReference;
    private UUID bidderId;
    private String bidderName;
    private String scenarioName;
    private String scenarioDescription;
    private String entityResolutionStatus;
    private String complianceStatus;
    private String temporalStatus;
    private String overallRiskLevel;
    private Integer overallRiskScore;
    private String decisionRecommendation;

}
