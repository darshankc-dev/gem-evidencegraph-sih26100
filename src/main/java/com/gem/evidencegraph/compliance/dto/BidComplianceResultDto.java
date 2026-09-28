package com.gem.evidencegraph.compliance.dto;

import com.gem.evidencegraph.compliance.OverallComplianceStatus;
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
public class BidComplianceResultDto {

    private UUID bidId;
    private UUID tenderId;
    private UUID bidderId;
    private OverallComplianceStatus overallStatus;
    private Double compliancePercentage;

    private int mandatoryRequirementCount;
    private int compliantCount;
    private int nonCompliantCount;
    private int missingCount;
    private int unverifiedCount;
    private int contradictoryCount;
    private int pendingReviewCount;
    private int notApplicableCount;

    @Builder.Default
    private List<ComplianceEvaluationResultDto> requirementResults = new ArrayList<>();

    private String summary;
    private LocalDateTime evaluatedAt;

}
