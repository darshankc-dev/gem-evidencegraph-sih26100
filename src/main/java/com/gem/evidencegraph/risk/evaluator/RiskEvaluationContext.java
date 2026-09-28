package com.gem.evidencegraph.risk.evaluator;

import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Bidder;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.EntityResolutionResult;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.EvidenceRelationship;
import com.gem.evidencegraph.entity.Tender;
import com.gem.evidencegraph.temporal.dto.BidTemporalEvaluationResultDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskEvaluationContext {

    private Bid bid;
    private Tender tender;
    private Bidder bidder;

    @Builder.Default
    private List<Document> documents = new ArrayList<>();

    @Builder.Default
    private List<Claim> claims = new ArrayList<>();

    @Builder.Default
    private List<Evidence> evidences = new ArrayList<>();

    private BidComplianceResultDto complianceResult;
    private BidTemporalEvaluationResultDto temporalResult;

    @Builder.Default
    private List<EntityResolutionResult> entityResolutionResults = new ArrayList<>();

    @Builder.Default
    private List<EvidenceRelationship> evidenceRelationships = new ArrayList<>();

    private LocalDateTime evaluatedAt;

}
