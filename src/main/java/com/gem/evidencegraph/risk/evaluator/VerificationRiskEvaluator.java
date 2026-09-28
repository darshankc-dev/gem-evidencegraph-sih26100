package com.gem.evidencegraph.risk.evaluator;

import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.RiskFinding;
import com.gem.evidencegraph.entity.VerificationStatus;
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
public class VerificationRiskEvaluator {

    public DimensionEvaluationResult evaluate(RiskEvaluationContext context) {
        List<RiskFinding> findings = new ArrayList<>();
        List<Evidence> evidences = context.getEvidences();

        if (evidences != null) {
            for (Evidence ev : evidences) {
                VerificationStatus status = ev.getVerificationStatus();
                if (status == null) {
                    status = VerificationStatus.UNVERIFIED;
                }

                RiskSeverity severity;
                String code;
                String title;
                String description;

                switch (status) {
                    case VERIFIED -> {
                        severity = RiskSeverity.INFO;
                        code = "VERIFICATION-000";
                        title = "Evidence verified against authoritative registry";
                        description = String.format("Evidence for '%s' (%s) verified by %s.",
                                ev.getAttribute(), ev.getSubject(), ev.getSourceType());
                    }
                    case UNVERIFIED -> {
                        severity = RiskSeverity.MEDIUM;
                        code = "VERIFICATION-001";
                        title = "Evidence unverified against authoritative registry";
                        description = String.format("Evidence for '%s' (%s) from %s has not yet been verified.",
                                ev.getAttribute(), ev.getSubject(), ev.getSourceType());
                    }
                    case SOURCE_UNAVAILABLE -> {
                        severity = RiskSeverity.MEDIUM;
                        code = "VERIFICATION-002";
                        title = "Authoritative verification source unavailable";
                        description = String.format("Verification gateway source '%s' is temporarily unavailable. " +
                                "Verification could not be completed; this does not indicate bidder non-compliance.",
                                ev.getSourceType());
                    }
                    case PENDING_HUMAN_REVIEW -> {
                        severity = RiskSeverity.MEDIUM;
                        code = "VERIFICATION-003";
                        title = "Evidence requires manual officer verification";
                        description = String.format("Evidence for '%s' (%s) flagged for manual verification by procurement officer.",
                                ev.getAttribute(), ev.getSubject());
                    }
                    case MISMATCH -> {
                        severity = RiskSeverity.HIGH;
                        code = "VERIFICATION-004";
                        title = "Evidence mismatch with authoritative registry";
                        description = String.format("Submitted value for '%s' mismatches authoritative record from %s.",
                                ev.getAttribute(), ev.getSourceType());
                    }
                    case NOT_APPLICABLE -> {
                        severity = RiskSeverity.INFO;
                        code = "VERIFICATION-005";
                        title = "Verification not applicable";
                        description = String.format("Authoritative verification not applicable for attribute '%s'.", ev.getAttribute());
                    }
                    default -> {
                        severity = RiskSeverity.LOW;
                        code = "VERIFICATION-099";
                        title = "Verification status " + status;
                        description = "Status: " + status;
                    }
                }

                int weight = RiskConstants.getWeight(severity);
                String sourceId = ev.getId() != null ? ev.getId().toString() : "UNKNOWN";

                RiskFinding finding = RiskFinding.builder()
                        .bid(context.getBid())
                        .dimension(RiskDimension.VERIFICATION)
                        .severity(severity)
                        .code(code)
                        .title(title)
                        .description(description)
                        .sourceType(ev.getSourceType() != null ? ev.getSourceType().name() : "EVIDENCE")
                        .sourceId(sourceId)
                        .rule("RULE-" + code)
                        .weight(weight)
                        .build();

                findings.add(finding);
            }
        }

        int rawSum = findings.stream().mapToInt(RiskFinding::getWeight).sum();
        int cappedScore = RiskConstants.capScore(rawSum);
        RiskLevel level = RiskLevel.fromScore(cappedScore);

        String summary = String.format("Verification evaluated with score %d (%s) across %d evidence item(s).",
                cappedScore, level, evidences != null ? evidences.size() : 0);
        String reason = cappedScore == 0
                ? "All submitted evidence items are verified by authoritative registries."
                : String.format("Verification risk score of %d derived from %d finding(s).", cappedScore, findings.size());

        return DimensionEvaluationResult.builder()
                .dimension(RiskDimension.VERIFICATION)
                .riskScore(cappedScore)
                .riskLevel(level)
                .findingCount(findings.size())
                .summary(summary)
                .reason(reason)
                .findings(findings)
                .build();
    }

}
