package com.gem.evidencegraph.compliance.rule;

import com.gem.evidencegraph.compliance.ComplianceStatus;
import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.ComplianceRequirement;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.RequirementType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.VerificationStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Order(30)
public class DeclarationRequirementRule implements ComplianceRule {

    @Override
    public boolean supports(ComplianceRequirement requirement) {
        if (requirement == null) {
            return false;
        }
        if (requirement.getRequirementType() == RequirementType.DECLARATION) {
            return true;
        }
        String code = requirement.getRequirementCode() != null ? requirement.getRequirementCode().toUpperCase() : "";
        String name = requirement.getName() != null ? requirement.getName().toUpperCase() : "";
        return code.contains("DECLARATION") || name.contains("DECLARATION")
                || code.contains("MII") || name.contains("MAKE IN INDIA")
                || code.contains("LOCAL_CONTENT") || code.contains("BLACKLIST");
    }

    @Override
    public ComplianceEvaluationResultDto evaluate(ComplianceRequirement requirement, BidEvaluationContext context) {
        DocumentType targetDocType = determineDeclarationDocType(requirement);
        String claimField = determineClaimField(requirement);

        List<Document> matchedDocs = context.getDocumentsByType(targetDocType);
        List<Claim> matchedClaims = context.getClaimsByField(claimField);

        List<UUID> claimIds = matchedClaims.stream().map(Claim::getId).collect(Collectors.toList());

        if (matchedDocs.isEmpty() && matchedClaims.isEmpty()) {
            return ComplianceEvaluationResultDto.builder()
                    .requirementId(requirement.getId())
                    .requirementCode(requirement.getRequirementCode())
                    .requirementName(requirement.getName())
                    .requirementType(requirement.getRequirementType())
                    .mandatory(requirement.isMandatory())
                    .status(ComplianceStatus.MISSING)
                    .confidence(0.0)
                    .missingItems(List.of(requirement.getName()))
                    .reason(String.format("Mandatory declaration '%s' is missing from the submitted bid.", requirement.getName()))
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        if (targetDocType == DocumentType.BLACKLIST_DECLARATION || requirement.getRequirementCode().toUpperCase().contains("BLACKLIST")) {
            List<Evidence> debarmentEvidences = context.getEvidencesBySource(SourceType.DEBARMENT_LIST);
            boolean isDebarred = debarmentEvidences.stream()
                    .anyMatch(e -> e.getVerificationStatus() == VerificationStatus.VERIFIED && "DEBARRED".equalsIgnoreCase(e.getValue()));

            if (isDebarred) {
                return ComplianceEvaluationResultDto.builder()
                        .requirementId(requirement.getId())
                        .requirementCode(requirement.getRequirementCode())
                        .requirementName(requirement.getName())
                        .requirementType(requirement.getRequirementType())
                        .mandatory(requirement.isMandatory())
                        .status(ComplianceStatus.NON_COMPLIANT)
                        .confidence(1.0)
                        .reason("Bidder submitted non-blacklisting declaration, but external debarment registry confirms bidder is actively debarred.")
                        .evaluatedAt(LocalDateTime.now())
                        .build();
            }
        }

        if (requirement.getValidationRule() != null && requirement.getValidationRule().contains("minLocalContent")) {
            Double threshold = parseNumericThreshold(requirement.getValidationRule(), "minLocalContent");
            if (threshold != null) {
                for (Claim c : matchedClaims) {
                    try {
                        double localContent = Double.parseDouble(c.getNormalizedValue().replaceAll("[^0-9.]", ""));
                        if (localContent < threshold) {
                            return ComplianceEvaluationResultDto.builder()
                                    .requirementId(requirement.getId())
                                    .requirementCode(requirement.getRequirementCode())
                                    .requirementName(requirement.getName())
                                    .requirementType(requirement.getRequirementType())
                                    .mandatory(requirement.isMandatory())
                                    .status(ComplianceStatus.NON_COMPLIANT)
                                    .confidence(1.0)
                                    .supportingClaimIds(claimIds)
                                    .reason(String.format("Declared local content (%.1f%%) does not meet mandatory threshold (%.1f%%).", localContent, threshold))
                                    .evaluatedAt(LocalDateTime.now())
                                    .build();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
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
                .reason(String.format("Mandatory declaration '%s' is present and verified.", requirement.getName()))
                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    private DocumentType determineDeclarationDocType(ComplianceRequirement req) {
        if (req.getExpectedDocumentType() != null) {
            return req.getExpectedDocumentType();
        }
        String code = req.getRequirementCode() != null ? req.getRequirementCode().toUpperCase() : "";
        String name = req.getName() != null ? req.getName().toUpperCase() : "";
        if (code.contains("MII") || name.contains("MAKE IN INDIA") || code.contains("LOCAL_CONTENT")) {
            return DocumentType.MAKE_IN_INDIA_DOCUMENT;
        }
        if (code.contains("BLACKLIST") || name.contains("BLACKLIST") || code.contains("DEBAR")) {
            return DocumentType.BLACKLIST_DECLARATION;
        }
        return DocumentType.OTHER;
    }

    private String determineClaimField(ComplianceRequirement req) {
        String code = req.getRequirementCode() != null ? req.getRequirementCode().toUpperCase() : "";
        String name = req.getName() != null ? req.getName().toUpperCase() : "";
        if (code.contains("MII") || name.contains("MAKE IN INDIA") || code.contains("LOCAL_CONTENT")) {
            return "LOCAL_CONTENT_PERCENTAGE";
        }
        if (code.contains("BLACKLIST") || name.contains("BLACKLIST")) {
            return "NON_BLACKLISTED_DECLARATION";
        }
        return req.getRequirementCode();
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
