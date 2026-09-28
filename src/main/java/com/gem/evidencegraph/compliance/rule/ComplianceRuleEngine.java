package com.gem.evidencegraph.compliance.rule;

import com.gem.evidencegraph.compliance.ComplianceStatus;
import com.gem.evidencegraph.compliance.OverallComplianceStatus;
import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.entity.ComplianceRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ComplianceRuleEngine {

    private final List<ComplianceRule> rules;

    public ComplianceRuleEngine(List<ComplianceRule> rules) {
        this.rules = rules;
        log.info("Initialized ComplianceRuleEngine with {} compliance rules", rules.size());
    }

    public BidComplianceResultDto evaluate(BidEvaluationContext context, List<ComplianceRequirement> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return BidComplianceResultDto.builder()
                    .bidId(context.getBid() != null ? context.getBid().getId() : null)
                    .tenderId(context.getTender() != null ? context.getTender().getId() : null)
                    .bidderId(context.getBidder() != null ? context.getBidder().getId() : null)
                    .overallStatus(OverallComplianceStatus.REVIEW)
                    .compliancePercentage(0.0)
                    .mandatoryRequirementCount(0)
                    .summary("No compliance requirements configured for tender.")
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        List<ComplianceEvaluationResultDto> results = new ArrayList<>();
        int mandatoryCount = 0;
        int compliantCount = 0;
        int nonCompliantCount = 0;
        int missingCount = 0;
        int unverifiedCount = 0;
        int contradictoryCount = 0;
        int pendingReviewCount = 0;
        int notApplicableCount = 0;

        for (ComplianceRequirement req : requirements) {
            if (req.isMandatory()) {
                mandatoryCount++;
            }

            ComplianceRule matchedRule = rules.stream()
                    .filter(r -> r.supports(req))
                    .findFirst()
                    .orElse(null);

            ComplianceEvaluationResultDto result;
            if (matchedRule != null) {
                result = matchedRule.evaluate(req, context);
            } else {
                result = ComplianceEvaluationResultDto.builder()
                        .requirementId(req.getId())
                        .requirementCode(req.getRequirementCode())
                        .requirementName(req.getName())
                        .requirementType(req.getRequirementType())
                        .mandatory(req.isMandatory())
                        .status(ComplianceStatus.PENDING_HUMAN_REVIEW)
                        .confidence(0.5)
                        .reason(String.format("No automated rule available for requirement '%s'. Marked for human review.", req.getName()))
                        .evaluatedAt(LocalDateTime.now())
                        .build();
            }

            results.add(result);

            switch (result.getStatus()) {
                case COMPLIANT -> compliantCount++;
                case NON_COMPLIANT -> nonCompliantCount++;
                case MISSING -> missingCount++;
                case UNVERIFIED -> unverifiedCount++;
                case CONTRADICTORY -> contradictoryCount++;
                case PENDING_HUMAN_REVIEW -> pendingReviewCount++;
                case NOT_APPLICABLE -> notApplicableCount++;
            }
        }

        int applicableCount = requirements.size() - notApplicableCount;
        double compliancePercentage = 0.0;
        if (applicableCount > 0) {
            double rawPercentage = ((double) compliantCount / applicableCount) * 100.0;
            compliancePercentage = Math.round(rawPercentage * 10.0) / 10.0;
        }

        OverallComplianceStatus overallStatus = determineOverallStatus(results);

        String summary = generateSummary(overallStatus, compliancePercentage, mandatoryCount,
                compliantCount, nonCompliantCount, missingCount, unverifiedCount, contradictoryCount, pendingReviewCount);

        return BidComplianceResultDto.builder()
                .bidId(context.getBid() != null ? context.getBid().getId() : null)
                .tenderId(context.getTender() != null ? context.getTender().getId() : null)
                .bidderId(context.getBidder() != null ? context.getBidder().getId() : null)
                .overallStatus(overallStatus)
                .compliancePercentage(compliancePercentage)
                .mandatoryRequirementCount(mandatoryCount)
                .compliantCount(compliantCount)
                .nonCompliantCount(nonCompliantCount)
                .missingCount(missingCount)
                .unverifiedCount(unverifiedCount)
                .contradictoryCount(contradictoryCount)
                .pendingReviewCount(pendingReviewCount)
                .notApplicableCount(notApplicableCount)
                .requirementResults(results)
                .summary(summary)
                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    private OverallComplianceStatus determineOverallStatus(List<ComplianceEvaluationResultDto> results) {
        boolean hasMandatoryNonCompliant = results.stream()
                .anyMatch(r -> r.isMandatory() && r.getStatus() == ComplianceStatus.NON_COMPLIANT);

        if (hasMandatoryNonCompliant) {
            return OverallComplianceStatus.NON_COMPLIANT;
        }

        boolean hasMandatoryReview = results.stream()
                .anyMatch(r -> r.isMandatory() && (
                        r.getStatus() == ComplianceStatus.MISSING
                                || r.getStatus() == ComplianceStatus.UNVERIFIED
                                || r.getStatus() == ComplianceStatus.CONTRADICTORY
                                || r.getStatus() == ComplianceStatus.PENDING_HUMAN_REVIEW
                ));

        if (hasMandatoryReview) {
            return OverallComplianceStatus.REVIEW;
        }

        boolean allMandatoryCompliant = results.stream()
                .filter(ComplianceEvaluationResultDto::isMandatory)
                .allMatch(r -> r.getStatus() == ComplianceStatus.COMPLIANT || r.getStatus() == ComplianceStatus.NOT_APPLICABLE);

        if (allMandatoryCompliant && !results.isEmpty()) {
            return OverallComplianceStatus.COMPLIANT;
        }

        return OverallComplianceStatus.REVIEW;
    }

    private String generateSummary(
            OverallComplianceStatus overallStatus,
            double compliancePercentage,
            int mandatoryCount,
            int compliantCount,
            int nonCompliantCount,
            int missingCount,
            int unverifiedCount,
            int contradictoryCount,
            int pendingReviewCount) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Overall Status: %s (%.1f%% compliant). ", overallStatus, compliancePercentage));
        sb.append(String.format("Satisfied %d mandatory requirements. ", compliantCount));

        if (nonCompliantCount > 0) {
            sb.append(String.format("CRITICAL: %d requirement(s) failed deterministically. ", nonCompliantCount));
        }
        if (contradictoryCount > 0) {
            sb.append(String.format("CONTRADICTION: %d requirement(s) contain conflicting evidence. ", contradictoryCount));
        }
        if (missingCount > 0) {
            sb.append(String.format("MISSING: %d requirement(s) have absent evidence. ", missingCount));
        }
        if (unverifiedCount > 0) {
            sb.append(String.format("UNVERIFIED: %d requirement(s) pending external source availability. ", unverifiedCount));
        }
        if (pendingReviewCount > 0) {
            sb.append(String.format("REVIEW: %d requirement(s) flagged for human verification. ", pendingReviewCount));
        }

        sb.append("Advisory assessment only; final qualification decision rests with the procurement officer.");
        return sb.toString();
    }

}
