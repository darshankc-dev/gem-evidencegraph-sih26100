package com.gem.evidencegraph.risk.evaluator;

import com.gem.evidencegraph.compliance.ComplianceStatus;
import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.entity.RiskFinding;
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
public class EligibilityRiskEvaluator {

    public DimensionEvaluationResult evaluate(RiskEvaluationContext context) {
        List<RiskFinding> findings = new ArrayList<>();
        BidComplianceResultDto complianceResult = context.getComplianceResult();

        if (complianceResult != null && complianceResult.getRequirementResults() != null) {
            for (ComplianceEvaluationResultDto reqResult : complianceResult.getRequirementResults()) {
                ComplianceStatus status = reqResult.getStatus();
                if (status == null) {
                    continue;
                }

                boolean mandatory = reqResult.isMandatory();
                RiskSeverity severity;
                String code;
                String title;
                String description;

                switch (status) {
                    case COMPLIANT -> {
                        severity = RiskSeverity.INFO;
                        code = "ELIGIBILITY-000";
                        title = "Compliance requirement satisfied";
                        description = String.format("Requirement '%s' (%s) is fully compliant.",
                                reqResult.getRequirementName(), reqResult.getRequirementCode());
                    }
                    case NON_COMPLIANT -> {
                        if (mandatory) {
                            severity = RiskSeverity.CRITICAL;
                            code = "ELIGIBILITY-001";
                            title = "Mandatory requirement non-compliant";
                            description = String.format("Mandatory requirement '%s' (%s) failed compliance: %s",
                                    reqResult.getRequirementName(), reqResult.getRequirementCode(),
                                    reqResult.getReason() != null ? reqResult.getReason() : "Non-compliant condition detected.");
                        } else {
                            severity = RiskSeverity.HIGH;
                            code = "ELIGIBILITY-002";
                            title = "Non-mandatory requirement non-compliant";
                            description = String.format("Non-mandatory requirement '%s' (%s) failed compliance: %s",
                                    reqResult.getRequirementName(), reqResult.getRequirementCode(),
                                    reqResult.getReason() != null ? reqResult.getReason() : "Non-compliant condition detected.");
                        }
                    }
                    case MISSING -> {
                        if (mandatory) {
                            severity = RiskSeverity.HIGH;
                            code = "ELIGIBILITY-003";
                            title = "Mandatory requirement documentation missing";
                            description = String.format("Mandatory requirement '%s' (%s) documentation or claim is missing.",
                                    reqResult.getRequirementName(), reqResult.getRequirementCode());
                        } else {
                            severity = RiskSeverity.MEDIUM;
                            code = "ELIGIBILITY-004";
                            title = "Non-mandatory requirement documentation missing";
                            description = String.format("Non-mandatory requirement '%s' (%s) documentation is missing.",
                                    reqResult.getRequirementName(), reqResult.getRequirementCode());
                        }
                    }
                    case UNVERIFIED -> {
                        severity = RiskSeverity.MEDIUM;
                        code = "ELIGIBILITY-005";
                        title = "Requirement evidence unverified";
                        description = String.format("Evidence supporting requirement '%s' (%s) remains unverified: %s",
                                reqResult.getRequirementName(), reqResult.getRequirementCode(),
                                reqResult.getReason() != null ? reqResult.getReason() : "Verification pending or inconclusive.");
                    }
                    case CONTRADICTORY -> {
                        severity = RiskSeverity.HIGH;
                        code = "ELIGIBILITY-006";
                        title = "Contradictory requirement evidence";
                        description = String.format("Contradictory evidence detected for requirement '%s' (%s): %s",
                                reqResult.getRequirementName(), reqResult.getRequirementCode(),
                                reqResult.getReason() != null ? reqResult.getReason() : "Conflicting evidence findings.");
                    }
                    case PENDING_HUMAN_REVIEW -> {
                        severity = RiskSeverity.MEDIUM;
                        code = "ELIGIBILITY-007";
                        title = "Requirement pending human review";
                        description = String.format("Requirement '%s' (%s) marked for manual procurement officer review: %s",
                                reqResult.getRequirementName(), reqResult.getRequirementCode(),
                                reqResult.getReason() != null ? reqResult.getReason() : "Human review trigger.");
                    }
                    case NOT_APPLICABLE -> {
                        severity = RiskSeverity.INFO;
                        code = "ELIGIBILITY-008";
                        title = "Requirement not applicable";
                        description = String.format("Requirement '%s' (%s) is not applicable.",
                                reqResult.getRequirementName(), reqResult.getRequirementCode());
                    }
                    default -> {
                        severity = RiskSeverity.INFO;
                        code = "ELIGIBILITY-099";
                        title = "Requirement status " + status;
                        description = "Status evaluated as: " + status;
                    }
                }

                int weight = RiskConstants.getWeight(severity);
                String sourceId = reqResult.getRequirementId() != null
                        ? reqResult.getRequirementId().toString()
                        : reqResult.getRequirementCode();

                RiskFinding finding = RiskFinding.builder()
                        .bid(context.getBid())
                        .dimension(RiskDimension.ELIGIBILITY)
                        .severity(severity)
                        .code(code)
                        .title(title)
                        .description(description)
                        .sourceType("COMPLIANCE_REQUIREMENT")
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

        String summary = String.format("Eligibility evaluated with score %d (%s) across %d requirement finding(s).",
                cappedScore, level, findings.size());
        String reason = cappedScore == 0
                ? "All evaluated tender compliance requirements are satisfied or not applicable."
                : String.format("Eligibility risk score of %d derived from %d requirement finding(s).", cappedScore, findings.size());

        return DimensionEvaluationResult.builder()
                .dimension(RiskDimension.ELIGIBILITY)
                .riskScore(cappedScore)
                .riskLevel(level)
                .findingCount(findings.size())
                .summary(summary)
                .reason(reason)
                .findings(findings)
                .build();
    }

}
