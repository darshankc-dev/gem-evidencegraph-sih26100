package com.gem.evidencegraph.compliance.rule;

import com.gem.evidencegraph.compliance.ComplianceStatus;
import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.ComplianceRequirement;
import com.gem.evidencegraph.entity.RequirementType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Order(40)
public class EligibilityRequirementRule implements ComplianceRule {

    @Override
    public boolean supports(ComplianceRequirement requirement) {
        if (requirement == null) {
            return false;
        }
        return requirement.getRequirementType() == RequirementType.ELIGIBILITY
                || requirement.getRequirementType() == RequirementType.TEMPORAL
                || requirement.getRequirementType() == RequirementType.OTHER;
    }

    @Override
    public ComplianceEvaluationResultDto evaluate(ComplianceRequirement requirement, BidEvaluationContext context) {
        String ruleConfig = requirement.getValidationRule();

        if (ruleConfig == null || ruleConfig.isBlank()) {
            return ComplianceEvaluationResultDto.builder()
                    .requirementId(requirement.getId())
                    .requirementCode(requirement.getRequirementCode())
                    .requirementName(requirement.getName())
                    .requirementType(requirement.getRequirementType())
                    .mandatory(requirement.isMandatory())
                    .status(ComplianceStatus.PENDING_HUMAN_REVIEW)
                    .confidence(0.5)
                    .reason(String.format("Eligibility condition for '%s' requires manual procurement officer evaluation.", requirement.getName()))
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        if (ruleConfig.contains("minExperienceYears")) {
            Double minExp = parseNumericThreshold(ruleConfig, "minExperienceYears");
            List<Claim> expClaims = context.getClaimsByField("EXPERIENCE_YEARS");
            if (expClaims.isEmpty()) {
                expClaims = context.getClaimsByField("YEARS_OF_EXPERIENCE");
            }

            if (expClaims.isEmpty()) {
                return ComplianceEvaluationResultDto.builder()
                        .requirementId(requirement.getId())
                        .requirementCode(requirement.getRequirementCode())
                        .requirementName(requirement.getName())
                        .requirementType(requirement.getRequirementType())
                        .mandatory(requirement.isMandatory())
                        .status(ComplianceStatus.MISSING)
                        .confidence(0.0)
                        .missingItems(List.of("EXPERIENCE_YEARS"))
                        .reason("Experience declaration/claim is missing from the submitted bid.")
                        .evaluatedAt(LocalDateTime.now())
                        .build();
            }

            List<UUID> claimIds = expClaims.stream().map(Claim::getId).collect(Collectors.toList());
            try {
                double claimedExp = Double.parseDouble(expClaims.get(0).getNormalizedValue().replaceAll("[^0-9.]", ""));
                if (claimedExp < minExp) {
                    return ComplianceEvaluationResultDto.builder()
                            .requirementId(requirement.getId())
                            .requirementCode(requirement.getRequirementCode())
                            .requirementName(requirement.getName())
                            .requirementType(requirement.getRequirementType())
                            .mandatory(requirement.isMandatory())
                            .status(ComplianceStatus.NON_COMPLIANT)
                            .confidence(1.0)
                            .supportingClaimIds(claimIds)
                            .reason(String.format("Claimed experience (%.1f years) is below mandatory requirement (%.1f years).", claimedExp, minExp))
                            .evaluatedAt(LocalDateTime.now())
                            .build();
                }

                return ComplianceEvaluationResultDto.builder()
                        .requirementId(requirement.getId())
                        .requirementCode(requirement.getRequirementCode())
                        .requirementName(requirement.getName())
                        .requirementType(requirement.getRequirementType())
                        .mandatory(requirement.isMandatory())
                        .status(ComplianceStatus.COMPLIANT)
                        .confidence(1.0)
                        .supportingClaimIds(claimIds)
                        .reason(String.format("Claimed experience (%.1f years) satisfies requirement (>= %.1f years).", claimedExp, minExp))
                        .evaluatedAt(LocalDateTime.now())
                        .build();
            } catch (Exception e) {
                return ComplianceEvaluationResultDto.builder()
                        .requirementId(requirement.getId())
                        .requirementCode(requirement.getRequirementCode())
                        .requirementName(requirement.getName())
                        .requirementType(requirement.getRequirementType())
                        .mandatory(requirement.isMandatory())
                        .status(ComplianceStatus.PENDING_HUMAN_REVIEW)
                        .confidence(0.5)
                        .supportingClaimIds(claimIds)
                        .reason("Could not deterministically parse experience claim value. Marked for human review.")
                        .evaluatedAt(LocalDateTime.now())
                        .build();
            }
        }

        if (ruleConfig.contains("minTurnover")) {
            Double minTurnover = parseNumericThreshold(ruleConfig, "minTurnover");
            List<Claim> turnoverClaims = context.getClaimsByField("ANNUAL_TURNOVER");
            if (turnoverClaims.isEmpty()) {
                turnoverClaims = context.getClaimsByField("TURNOVER");
            }

            if (turnoverClaims.isEmpty()) {
                return ComplianceEvaluationResultDto.builder()
                        .requirementId(requirement.getId())
                        .requirementCode(requirement.getRequirementCode())
                        .requirementName(requirement.getName())
                        .requirementType(requirement.getRequirementType())
                        .mandatory(requirement.isMandatory())
                        .status(ComplianceStatus.MISSING)
                        .confidence(0.0)
                        .missingItems(List.of("ANNUAL_TURNOVER"))
                        .reason("Annual turnover claim is missing from the submitted bid.")
                        .evaluatedAt(LocalDateTime.now())
                        .build();
            }

            List<UUID> claimIds = turnoverClaims.stream().map(Claim::getId).collect(Collectors.toList());
            try {
                double claimedTurnover = Double.parseDouble(turnoverClaims.get(0).getNormalizedValue().replaceAll("[^0-9.]", ""));
                if (claimedTurnover < minTurnover) {
                    return ComplianceEvaluationResultDto.builder()
                            .requirementId(requirement.getId())
                            .requirementCode(requirement.getRequirementCode())
                            .requirementName(requirement.getName())
                            .requirementType(requirement.getRequirementType())
                            .mandatory(requirement.isMandatory())
                            .status(ComplianceStatus.NON_COMPLIANT)
                            .confidence(1.0)
                            .supportingClaimIds(claimIds)
                            .reason(String.format("Claimed annual turnover (₹%.2f) does not satisfy mandatory minimum turnover (₹%.2f).", claimedTurnover, minTurnover))
                            .evaluatedAt(LocalDateTime.now())
                            .build();
                }

                return ComplianceEvaluationResultDto.builder()
                        .requirementId(requirement.getId())
                        .requirementCode(requirement.getRequirementCode())
                        .requirementName(requirement.getName())
                        .requirementType(requirement.getRequirementType())
                        .mandatory(requirement.isMandatory())
                        .status(ComplianceStatus.COMPLIANT)
                        .confidence(1.0)
                        .supportingClaimIds(claimIds)
                        .reason(String.format("Claimed annual turnover (₹%.2f) satisfies minimum turnover (₹%.2f).", claimedTurnover, minTurnover))
                        .evaluatedAt(LocalDateTime.now())
                        .build();
            } catch (Exception e) {
                return ComplianceEvaluationResultDto.builder()
                        .requirementId(requirement.getId())
                        .requirementCode(requirement.getRequirementCode())
                        .requirementName(requirement.getName())
                        .requirementType(requirement.getRequirementType())
                        .mandatory(requirement.isMandatory())
                        .status(ComplianceStatus.PENDING_HUMAN_REVIEW)
                        .confidence(0.5)
                        .supportingClaimIds(claimIds)
                        .reason("Could not deterministically parse turnover claim value. Marked for human review.")
                        .evaluatedAt(LocalDateTime.now())
                        .build();
            }
        }

        return ComplianceEvaluationResultDto.builder()
                .requirementId(requirement.getId())
                .requirementCode(requirement.getRequirementCode())
                .requirementName(requirement.getName())
                .requirementType(requirement.getRequirementType())
                .mandatory(requirement.isMandatory())
                .status(ComplianceStatus.PENDING_HUMAN_REVIEW)
                .confidence(0.5)
                .reason(String.format("Tender requirement '%s' requires manual procurement evaluation.", requirement.getName()))
                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    private Double parseNumericThreshold(String rule, String key) {
        try {
            for (String part : rule.split("[,;]")) {
                String[] kv = part.split("[=:]");
                if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(key)) {
                    return Double.parseDouble(kv[1].trim());
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

}
