package com.gem.evidencegraph.risk.evaluator;

import com.gem.evidencegraph.entity.EntityResolutionResult;
import com.gem.evidencegraph.entity.EvidenceRelationship;
import com.gem.evidencegraph.entity.RelationshipType;
import com.gem.evidencegraph.entity.RiskFinding;
import com.gem.evidencegraph.entityresolution.EntityMatchStatus;
import com.gem.evidencegraph.risk.RiskConstants;
import com.gem.evidencegraph.risk.RiskDimension;
import com.gem.evidencegraph.risk.RiskLevel;
import com.gem.evidencegraph.risk.RiskSeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ConsistencyRiskEvaluator {

    public DimensionEvaluationResult evaluate(RiskEvaluationContext context) {
        List<RiskFinding> findings = new ArrayList<>();

        List<EntityResolutionResult> errList = context.getEntityResolutionResults();
        if (errList != null && !errList.isEmpty()) {
            for (EntityResolutionResult err : errList) {
                EntityMatchStatus matchStatus = err.getMatchStatus();
                if (matchStatus == null) {
                    continue;
                }

                RiskSeverity severity;
                String code;
                String title;
                String description;

                switch (matchStatus) {
                    case MATCH -> {
                        severity = RiskSeverity.INFO;
                        code = "CONSISTENCY-000";
                        title = "Bidder identity consistent";
                        description = "Statutory registry identifiers match submitted bidder profile.";
                    }
                    case PROBABLE_MATCH -> {
                        severity = RiskSeverity.LOW;
                        code = "CONSISTENCY-001";
                        title = "Bidder identity probable match";
                        description = "Bidder identity matches on primary attributes with minor variations in supporting data: "
                                + (err.getExplanation() != null ? err.getExplanation() : "Supporting attribute variance.");
                    }
                    case INSUFFICIENT_DATA -> {
                        severity = RiskSeverity.MEDIUM;
                        code = "CONSISTENCY-002";
                        title = "Inconclusive bidder identity data";
                        description = "Insufficient statutory identity attributes available to verify bidder profile consistency: "
                                + (err.getExplanation() != null ? err.getExplanation() : "Incomplete attributes.");
                    }
                    case MISMATCH -> {
                        severity = RiskSeverity.HIGH;
                        code = "CONSISTENCY-003";
                        title = "Conflicting bidder identity information";
                        description = "Conflicting bidder identity information detected across statutory records and submitted profile: "
                                + (err.getExplanation() != null ? err.getExplanation() : "Identity mismatch detected.");
                    }
                    default -> {
                        severity = RiskSeverity.LOW;
                        code = "CONSISTENCY-099";
                        title = "Entity resolution status " + matchStatus;
                        description = "Identity resolution evaluated as: " + matchStatus;
                    }
                }

                int weight = RiskConstants.getWeight(severity);
                String sourceId = err.getId() != null ? err.getId().toString() : "BIDDER-" + (context.getBidder() != null ? context.getBidder().getId() : "UNKNOWN");

                RiskFinding finding = RiskFinding.builder()
                        .bid(context.getBid())
                        .dimension(RiskDimension.CONSISTENCY)
                        .severity(severity)
                        .code(code)
                        .title(title)
                        .description(description)
                        .sourceType("ENTITY_RESOLUTION")
                        .sourceId(sourceId)
                        .rule("RULE-" + code)
                        .weight(weight)
                        .build();

                findings.add(finding);
            }
        }

        List<EvidenceRelationship> relationships = context.getEvidenceRelationships();
        if (relationships != null) {
            for (EvidenceRelationship rel : relationships) {
                if (rel.getRelationshipType() == RelationshipType.CONTRADICTS) {
                    RiskSeverity severity = RiskSeverity.HIGH;
                    int weight = RiskConstants.getWeight(severity);
                    String code = "CONSISTENCY-004";
                    String title = "Conflicting evidence detected";
                    String description = "Verified contradiction between evidence sources detected: "
                            + (rel.getReason() != null ? rel.getReason() : "Evidence values contradict each other.");

                    String sourceId = rel.getId() != null ? rel.getId().toString() : "REL-CONTRADICTS";

                    RiskFinding finding = RiskFinding.builder()
                        .bid(context.getBid())
                        .dimension(RiskDimension.CONSISTENCY)
                        .severity(severity)
                        .code(code)
                        .title(title)
                        .description(description)
                        .sourceType("EVIDENCE_RELATIONSHIP")
                        .sourceId(sourceId)
                        .rule("RULE-CONSISTENCY-CONTRADICTION")
                        .weight(weight)
                        .build();

                    findings.add(finding);
                }
            }
        }

        int rawSum = findings.stream().mapToInt(RiskFinding::getWeight).sum();
        int cappedScore = RiskConstants.capScore(rawSum);
        RiskLevel level = RiskLevel.fromScore(cappedScore);

        String summary = String.format("Consistency evaluated with score %d (%s) across %d finding(s).",
                cappedScore, level, findings.size());
        String reason = cappedScore == 0
                ? "Bidder identity and evidence sources exhibit full consistency without contradictions."
                : String.format("Consistency risk score of %d derived from %d finding(s).", cappedScore, findings.size());

        return DimensionEvaluationResult.builder()
                .dimension(RiskDimension.CONSISTENCY)
                .riskScore(cappedScore)
                .riskLevel(level)
                .findingCount(findings.size())
                .summary(summary)
                .reason(reason)
                .findings(findings)
                .build();
    }

}
