package com.gem.evidencegraph.risk.evaluator;

import com.gem.evidencegraph.entity.RiskFinding;
import com.gem.evidencegraph.risk.DecisionRecommendation;
import com.gem.evidencegraph.risk.RiskDimension;
import com.gem.evidencegraph.risk.RiskLevel;
import com.gem.evidencegraph.risk.RiskSeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class DecisionEngine {

    public DecisionOutcome evaluate(
            RiskEvaluationContext context,
            HardFailResult hardFailResult,
            List<RiskFinding> allFindings,
            int overallRiskScore,
            RiskLevel overallRiskLevel) {

        if (!hasSufficientEvidence(context)) {
            String reason = "Insufficient evidence: No submitted documents or verified external evidence available to conduct automated assessment.";
            String summary = "Recommendation: INSUFFICIENT_EVIDENCE. The bidder has submitted no documents or verified claims.";
            return DecisionOutcome.builder()
                    .recommendation(DecisionRecommendation.INSUFFICIENT_EVIDENCE)
                    .reason(reason)
                    .summary(summary)
                    .build();
        }

        if (hardFailResult.isHardFail()) {
            String reason = "Hard-fail condition detected: " + String.join("; ", hardFailResult.getReasons());
            String summary = "Recommendation: HIGH_RISK. Mandatory non-compliance or document integrity failure detected.";
            return DecisionOutcome.builder()
                    .recommendation(DecisionRecommendation.HIGH_RISK)
                    .reason(reason)
                    .summary(summary)
                    .build();
        }

        List<String> triggers = collectReviewTriggers(context, allFindings, overallRiskLevel);
        if (!triggers.isEmpty()) {
            String reason = "Review required: Unresolved items require procurement officer attention: "
                    + String.join("; ", triggers);
            String summary = String.format("Recommendation: REVIEW. %d item(s) flagged for procurement officer inspection.", triggers.size());
            return DecisionOutcome.builder()
                    .recommendation(DecisionRecommendation.REVIEW)
                    .reason(reason)
                    .summary(summary)
                    .reviewTriggers(triggers)
                    .build();
        }

        String reason = "Applicable deterministic checks have been satisfied and there are no unresolved review triggers or hard-fail findings.";
        String summary = "Recommendation: RECOMMEND. All evaluated dimensions satisfy deterministic thresholds.";
        return DecisionOutcome.builder()
                .recommendation(DecisionRecommendation.RECOMMEND)
                .reason(reason)
                .summary(summary)
                .build();
    }

    private boolean hasSufficientEvidence(RiskEvaluationContext context) {
        boolean hasDocs = context.getDocuments() != null && !context.getDocuments().isEmpty();
        boolean hasEvidence = context.getEvidences() != null && !context.getEvidences().isEmpty();
        return hasDocs || hasEvidence;
    }

    private List<String> collectReviewTriggers(
            RiskEvaluationContext context,
            List<RiskFinding> allFindings,
            RiskLevel overallRiskLevel) {

        Set<String> triggers = new LinkedHashSet<>();

        for (RiskFinding f : allFindings) {
            String code = f.getCode() != null ? f.getCode() : "";

            if (code.equals("ELIGIBILITY-006") || code.equals("CONSISTENCY-003") || code.equals("CONSISTENCY-004")) {
                triggers.add("Contradictory evidence detected (" + f.getTitle() + ")");
            }

            if (code.equals("ELIGIBILITY-005")) {
                triggers.add("Unverified compliance evidence (" + f.getTitle() + ")");
            }

            if (code.equals("VERIFICATION-002")) {
                triggers.add("External verification source unavailable (" + f.getTitle() + ")");
            }

            if (f.getDimension() == RiskDimension.TEMPORAL && f.getSeverity() != RiskSeverity.INFO) {
                triggers.add("Temporal concern detected (" + f.getTitle() + ")");
            }

            if (code.equals("ELIGIBILITY-007") || code.equals("DOC-INTEGRITY-002") || code.equals("VERIFICATION-003")) {
                triggers.add("Pending human verification required (" + f.getTitle() + ")");
            }

            if (code.equals("ELIGIBILITY-004") || code.equals("ELIGIBILITY-007")) {
                triggers.add("Ambiguous eligibility requirement (" + f.getTitle() + ")");
            }

            if (code.equals("ELIGIBILITY-003")) {
                triggers.add("Missing mandatory requirement evidence (" + f.getTitle() + ")");
            }

            if (code.equals("DOC-INTEGRITY-001") || code.equals("CONSISTENCY-002")) {
                triggers.add("Incomplete evidence set (" + f.getTitle() + ")");
            }

            if (code.equals("VERIFICATION-004")) {
                triggers.add("Registry verification mismatch (" + f.getTitle() + ")");
            }

            if (code.equals("ELIGIBILITY-002")) {
                triggers.add("Non-mandatory requirement non-compliant (" + f.getTitle() + ")");
            }
        }

        if (overallRiskLevel == RiskLevel.HIGH) {
            triggers.add("Overall cumulative risk level is HIGH, requiring procurement officer review.");
        }

        return new ArrayList<>(triggers);
    }

}
