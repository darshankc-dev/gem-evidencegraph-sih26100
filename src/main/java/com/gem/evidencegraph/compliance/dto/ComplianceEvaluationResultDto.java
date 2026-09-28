package com.gem.evidencegraph.compliance.dto;

import com.gem.evidencegraph.compliance.ComplianceStatus;
import com.gem.evidencegraph.entity.RequirementType;
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
public class ComplianceEvaluationResultDto {

    private UUID requirementId;
    private String requirementCode;
    private String requirementName;
    private RequirementType requirementType;
    private boolean mandatory;
    private ComplianceStatus status;
    private Double confidence;

    @Builder.Default
    private List<UUID> evidenceIds = new ArrayList<>();

    @Builder.Default
    private List<UUID> supportingClaimIds = new ArrayList<>();

    private String reason;

    @Builder.Default
    private List<String> missingItems = new ArrayList<>();

    @Builder.Default
    private List<String> contradictions = new ArrayList<>();

    private LocalDateTime evaluatedAt;

}
