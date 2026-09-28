package com.gem.evidencegraph.risk.evaluator;

import com.gem.evidencegraph.entity.RiskFinding;
import com.gem.evidencegraph.risk.RiskConstants;
import com.gem.evidencegraph.risk.RiskDimension;
import com.gem.evidencegraph.risk.RiskLevel;
import com.gem.evidencegraph.risk.RiskSeverity;
import com.gem.evidencegraph.temporal.TemporalStatus;
import com.gem.evidencegraph.temporal.dto.BidTemporalEvaluationResultDto;
import com.gem.evidencegraph.temporal.dto.TemporalEvaluationResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TemporalRiskEvaluator {

    public DimensionEvaluationResult evaluate(RiskEvaluationContext context) {
        List<RiskFinding> findings = new ArrayList<>();
        BidTemporalEvaluationResultDto temporalResult = context.getTemporalResult();

        if (temporalResult != null && temporalResult.getResults() != null) {
            for (TemporalEvaluationResponseDto item : temporalResult.getResults()) {
                TemporalStatus status = item.getStatus();
                if (status == null) {
                    continue;
                }

                RiskSeverity severity;
                String code;
                String title;
                String description;

                switch (status) {
                    case VALID_AT_BID_DATE -> {
                        severity = RiskSeverity.INFO;
                        code = "TEMPORAL-000";
                        title = "Evidence valid at bid date";
                        description = item.getReason() != null ? item.getReason() : "Evidence validity covers the bid submission date.";
                    }
                    case NOT_APPLICABLE -> {
                        severity = RiskSeverity.INFO;
                        code = "TEMPORAL-001";
                        title = "Temporal verification not applicable";
                        description = item.getReason() != null ? item.getReason() : "Evidence has no statutory expiration or validity boundaries.";
                    }
                    case MISSING_TEMPORAL_DATA -> {
                        severity = RiskSeverity.MEDIUM;
                        code = "TEMPORAL-002";
                        title = "Missing temporal validity dates";
                        description = item.getReason() != null ? item.getReason() : "Evidence lacks issue or expiry dates required for full temporal verification.";
                    }
                    case ISSUED_AFTER_TENDER_PUBLICATION -> {
                        severity = RiskSeverity.MEDIUM;
                        code = "TEMPORAL-003";
                        title = "Evidence issued after tender publication";
                        description = item.getReason() != null ? item.getReason() : "Evidence was issued after tender publication date.";
                    }
                    case ISSUED_AFTER_BID -> {
                        severity = RiskSeverity.HIGH;
                        code = "TEMPORAL-004";
                        title = "Evidence issued after bid submission";
                        description = item.getReason() != null ? item.getReason() : "Evidence was issued after the bid was submitted.";
                    }
                    case NOT_YET_VALID_AT_BID_DATE -> {
                        severity = RiskSeverity.HIGH;
                        code = "TEMPORAL-005";
                        title = "Evidence not yet valid at bid date";
                        description = item.getReason() != null ? item.getReason() : "Evidence validity window starts after the bid submission date.";
                    }
                    case EXPIRED_AT_BID_DATE -> {
                        severity = RiskSeverity.HIGH;
                        code = "TEMPORAL-006";
                        title = "Evidence expired at bid date";
                        description = item.getReason() != null ? item.getReason() : "Evidence validity ended before the bid submission date.";
                    }
                    case CONFLICTING_DATES -> {
                        severity = RiskSeverity.HIGH;
                        code = "TEMPORAL-007";
                        title = "Conflicting temporal dates";
                        description = item.getReason() != null ? item.getReason() : "Contradictory dates observed across evidence source and documents.";
                    }
                    default -> {
                        severity = RiskSeverity.INFO;
                        code = "TEMPORAL-099";
                        title = "Temporal status " + status;
                        description = "Status: " + status;
                    }
                }

                int weight = RiskConstants.getWeight(severity);
                String sourceId = item.getEvidenceId() != null
                        ? item.getEvidenceId().toString()
                        : (item.getEvaluationId() != null ? item.getEvaluationId().toString() : "UNKNOWN");

                RiskFinding finding = RiskFinding.builder()
                        .bid(context.getBid())
                        .dimension(RiskDimension.TEMPORAL)
                        .severity(severity)
                        .code(code)
                        .title(title)
                        .description(description)
                        .sourceType("TEMPORAL_EVALUATION")
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

        String summary = String.format("Temporal validity evaluated with score %d (%s) across %d evidence item(s).",
                cappedScore, level, findings.size());
        String reason = cappedScore == 0
                ? "All evaluated evidence items were valid at the time of bid submission."
                : String.format("Temporal risk score of %d derived from %d finding(s).", cappedScore, findings.size());

        return DimensionEvaluationResult.builder()
                .dimension(RiskDimension.TEMPORAL)
                .riskScore(cappedScore)
                .riskLevel(level)
                .findingCount(findings.size())
                .summary(summary)
                .reason(reason)
                .findings(findings)
                .build();
    }

}
