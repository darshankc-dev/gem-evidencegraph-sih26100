package com.gem.evidencegraph.compliance.rule;

import com.gem.evidencegraph.compliance.ComplianceStatus;
import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.ComplianceRequirement;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.RequirementType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.VerificationStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Order(10)
public class RegistrationRequirementRule implements ComplianceRule {

    @Override
    public boolean supports(ComplianceRequirement requirement) {
        if (requirement == null) {
            return false;
        }
        if (requirement.getRequirementType() == RequirementType.REGISTRATION) {
            return true;
        }
        SourceType source = requirement.getExpectedSourceType();
        if (source == SourceType.GST || source == SourceType.PAN || source == SourceType.UDYAM || source == SourceType.DEBARMENT_LIST) {
            return true;
        }
        String code = requirement.getRequirementCode() != null ? requirement.getRequirementCode().toUpperCase() : "";
        String name = requirement.getName() != null ? requirement.getName().toUpperCase() : "";
        return code.contains("GST") || code.contains("PAN") || code.contains("UDYAM")
                || name.contains("GST") || name.contains("PAN") || name.contains("UDYAM");
    }

    @Override
    public ComplianceEvaluationResultDto evaluate(ComplianceRequirement requirement, BidEvaluationContext context) {
        SourceType targetSource = determineTargetSource(requirement);
        String targetAttribute = determineTargetAttribute(targetSource, requirement);

        List<Claim> relevantClaims = context.getClaimsByField(targetAttribute);
        List<Evidence> relevantEvidences = new ArrayList<>(context.getEvidencesBySource(targetSource));
        if (relevantEvidences.isEmpty() && targetAttribute != null) {
            relevantEvidences.addAll(context.getEvidencesByAttribute(targetAttribute));
        }

        List<UUID> evidenceIds = relevantEvidences.stream().map(Evidence::getId).collect(Collectors.toList());
        List<UUID> claimIds = relevantClaims.stream().map(Claim::getId).collect(Collectors.toList());

        if (relevantClaims.isEmpty() && relevantEvidences.isEmpty()) {
            return ComplianceEvaluationResultDto.builder()
                    .requirementId(requirement.getId())
                    .requirementCode(requirement.getRequirementCode())
                    .requirementName(requirement.getName())
                    .requirementType(requirement.getRequirementType())
                    .mandatory(requirement.isMandatory())
                    .status(ComplianceStatus.MISSING)
                    .confidence(0.0)
                    .missingItems(List.of(targetAttribute != null ? targetAttribute : requirement.getRequirementCode()))
                    .reason(String.format("Statutory registration evidence for '%s' is missing from the submitted bid.", requirement.getName()))
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        Set<String> distinctValues = new HashSet<>();
        boolean hasMismatchStatus = false;
        boolean hasVerifiedStatus = false;
        boolean hasUnavailableStatus = false;
        boolean isDebarred = false;

        for (Evidence ev : relevantEvidences) {
            String val = ev.getNormalizedValue() != null ? ev.getNormalizedValue() : ev.getValue();
            if (val != null && !val.isBlank()) {
                distinctValues.add(val.trim().toUpperCase());
            }
            if (ev.getVerificationStatus() == VerificationStatus.MISMATCH) {
                hasMismatchStatus = true;
            }
            if (ev.getVerificationStatus() == VerificationStatus.VERIFIED) {
                hasVerifiedStatus = true;
                if (targetSource == SourceType.DEBARMENT_LIST && "DEBARRED".equalsIgnoreCase(val)) {
                    isDebarred = true;
                }
            }
            if (ev.getVerificationStatus() == VerificationStatus.SOURCE_UNAVAILABLE || ev.getVerificationStatus() == VerificationStatus.UNVERIFIED) {
                hasUnavailableStatus = true;
            }
        }

        if (isDebarred) {
            return ComplianceEvaluationResultDto.builder()
                    .requirementId(requirement.getId())
                    .requirementCode(requirement.getRequirementCode())
                    .requirementName(requirement.getName())
                    .requirementType(requirement.getRequirementType())
                    .mandatory(requirement.isMandatory())
                    .status(ComplianceStatus.NON_COMPLIANT)
                    .confidence(1.0)
                    .evidenceIds(evidenceIds)
                    .supportingClaimIds(claimIds)
                    .reason("Bidder is actively debarred/blacklisted according to official debarment records.")
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        if (distinctValues.size() > 1 || hasMismatchStatus) {
            List<String> contradictions = new ArrayList<>();
            if (distinctValues.size() > 1) {
                contradictions.add(String.format("Multiple conflicting evidence values found: %s", distinctValues));
            }
            if (hasMismatchStatus) {
                contradictions.add("Observed external registry record conflicts with claimed registration value.");
            }

            return ComplianceEvaluationResultDto.builder()
                    .requirementId(requirement.getId())
                    .requirementCode(requirement.getRequirementCode())
                    .requirementName(requirement.getName())
                    .requirementType(requirement.getRequirementType())
                    .mandatory(requirement.isMandatory())
                    .status(ComplianceStatus.CONTRADICTORY)
                    .confidence(1.0)
                    .evidenceIds(evidenceIds)
                    .supportingClaimIds(claimIds)
                    .contradictions(contradictions)
                    .reason(String.format("Registration evidence for '%s' contains contradictions or registry mismatch.", requirement.getName()))
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        if (hasVerifiedStatus) {
            return ComplianceEvaluationResultDto.builder()
                    .requirementId(requirement.getId())
                    .requirementCode(requirement.getRequirementCode())
                    .requirementName(requirement.getName())
                    .requirementType(requirement.getRequirementType())
                    .mandatory(requirement.isMandatory())
                    .status(ComplianceStatus.COMPLIANT)
                    .confidence(1.0)
                    .evidenceIds(evidenceIds)
                    .supportingClaimIds(claimIds)
                    .reason(String.format("Registration evidence for '%s' is present and verified with external registry.", requirement.getName()))
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        if (hasUnavailableStatus || !relevantClaims.isEmpty()) {
            return ComplianceEvaluationResultDto.builder()
                    .requirementId(requirement.getId())
                    .requirementCode(requirement.getRequirementCode())
                    .requirementName(requirement.getName())
                    .requirementType(requirement.getRequirementType())
                    .mandatory(requirement.isMandatory())
                    .status(ComplianceStatus.UNVERIFIED)
                    .confidence(0.0)
                    .evidenceIds(evidenceIds)
                    .supportingClaimIds(claimIds)
                    .reason(String.format("Registration claim exists for '%s', but external verification source is currently unavailable or record not found.", requirement.getName()))
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        return ComplianceEvaluationResultDto.builder()
                .requirementId(requirement.getId())
                .requirementCode(requirement.getRequirementCode())
                .requirementName(requirement.getName())
                .requirementType(requirement.getRequirementType())
                .mandatory(requirement.isMandatory())
                .status(ComplianceStatus.MISSING)
                .confidence(0.0)
                .reason(String.format("Registration requirement '%s' cannot be confirmed.", requirement.getName()))
                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    private SourceType determineTargetSource(ComplianceRequirement req) {
        if (req.getExpectedSourceType() != null) {
            return req.getExpectedSourceType();
        }
        String code = req.getRequirementCode() != null ? req.getRequirementCode().toUpperCase() : "";
        String name = req.getName() != null ? req.getName().toUpperCase() : "";
        if (code.contains("GST") || name.contains("GST")) {
            return SourceType.GST;
        }
        if (code.contains("PAN") || name.contains("PAN")) {
            return SourceType.PAN;
        }
        if (code.contains("UDYAM") || name.contains("UDYAM")) {
            return SourceType.UDYAM;
        }
        if (code.contains("DEBAR") || name.contains("DEBAR") || code.contains("BLACK") || name.contains("BLACK")) {
            return SourceType.DEBARMENT_LIST;
        }
        return SourceType.OTHER;
    }

    private String determineTargetAttribute(SourceType source, ComplianceRequirement req) {
        if (source == SourceType.GST) {
            return "GSTIN";
        }
        if (source == SourceType.PAN) {
            return "PAN";
        }
        if (source == SourceType.UDYAM) {
            return "UDYAM_NUMBER";
        }
        if (source == SourceType.DEBARMENT_LIST) {
            return "DEBARMENT";
        }
        return req.getRequirementCode();
    }

}
