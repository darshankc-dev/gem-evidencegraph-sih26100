package com.gem.evidencegraph.compliance.rule;

import com.gem.evidencegraph.compliance.ComplianceStatus;
import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.entity.ComplianceRequirement;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.IntegrityStatus;
import com.gem.evidencegraph.entity.RequirementType;
import com.gem.evidencegraph.entity.VerificationStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Order(20)
public class DocumentRequirementRule implements ComplianceRule {

    @Override
    public boolean supports(ComplianceRequirement requirement) {
        if (requirement == null) {
            return false;
        }
        if (requirement.getRequirementType() == RequirementType.DOCUMENT) {
            return true;
        }
        if (requirement.getExpectedDocumentType() != null) {
            return true;
        }
        String code = requirement.getRequirementCode() != null ? requirement.getRequirementCode().toUpperCase() : "";
        String name = requirement.getName() != null ? requirement.getName().toUpperCase() : "";
        return code.contains("OEM") || name.contains("OEM") || code.contains("DOC") || name.contains("DOCUMENT");
    }

    @Override
    public ComplianceEvaluationResultDto evaluate(ComplianceRequirement requirement, BidEvaluationContext context) {
        DocumentType targetDocType = determineTargetDocumentType(requirement);

        List<Document> matchedDocs = context.getDocumentsByType(targetDocType);
        if (matchedDocs.isEmpty()) {

            if (targetDocType == DocumentType.OTHER) {
                matchedDocs = context.getDocuments();
            }
        }

        if (matchedDocs.isEmpty()) {
            return ComplianceEvaluationResultDto.builder()
                    .requirementId(requirement.getId())
                    .requirementCode(requirement.getRequirementCode())
                    .requirementName(requirement.getName())
                    .requirementType(requirement.getRequirementType())
                    .mandatory(requirement.isMandatory())
                    .status(ComplianceStatus.MISSING)
                    .confidence(0.0)
                    .missingItems(List.of(targetDocType.name()))
                    .reason(String.format("Required document '%s' (type: %s) is missing from the submitted bid.", requirement.getName(), targetDocType))
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        List<Document> corruptedDocs = matchedDocs.stream()
                .filter(d -> d.getIntegrityStatus() == IntegrityStatus.FAILED)
                .collect(Collectors.toList());

        if (!corruptedDocs.isEmpty()) {
            return ComplianceEvaluationResultDto.builder()
                    .requirementId(requirement.getId())
                    .requirementCode(requirement.getRequirementCode())
                    .requirementName(requirement.getName())
                    .requirementType(requirement.getRequirementType())
                    .mandatory(requirement.isMandatory())
                    .status(ComplianceStatus.NON_COMPLIANT)
                    .confidence(1.0)
                    .reason(String.format("Submitted document '%s' failed integrity validation or is corrupted.", corruptedDocs.get(0).getOriginalFileName()))
                    .evaluatedAt(LocalDateTime.now())
                    .build();
        }

        if (requirement.getExpectedSourceType() != null) {
            List<Evidence> evidences = context.getEvidencesBySource(requirement.getExpectedSourceType());
            List<UUID> evidenceIds = evidences.stream().map(Evidence::getId).collect(Collectors.toList());

            boolean hasVerified = evidences.stream().anyMatch(e -> e.getVerificationStatus() == VerificationStatus.VERIFIED);
            boolean hasMismatch = evidences.stream().anyMatch(e -> e.getVerificationStatus() == VerificationStatus.MISMATCH);
            boolean hasUnavailable = evidences.stream().anyMatch(e -> e.getVerificationStatus() == VerificationStatus.SOURCE_UNAVAILABLE || e.getVerificationStatus() == VerificationStatus.UNVERIFIED);

            if (hasMismatch) {
                return ComplianceEvaluationResultDto.builder()
                        .requirementId(requirement.getId())
                        .requirementCode(requirement.getRequirementCode())
                        .requirementName(requirement.getName())
                        .requirementType(requirement.getRequirementType())
                        .mandatory(requirement.isMandatory())
                        .status(ComplianceStatus.CONTRADICTORY)
                        .confidence(1.0)
                        .evidenceIds(evidenceIds)
                        .contradictions(List.of("Document claims contradict external verification records."))
                        .reason(String.format("Document '%s' claims contradict external verification records.", requirement.getName()))
                        .evaluatedAt(LocalDateTime.now())
                        .build();
            }

            if (hasVerified) {
                return ComplianceEvaluationResultDto.builder()
                        .requirementId(requirement.getId())
                        .requirementCode(requirement.getRequirementCode())
                        .requirementName(requirement.getName())
                        .requirementType(requirement.getRequirementType())
                        .mandatory(requirement.isMandatory())
                        .status(ComplianceStatus.COMPLIANT)
                        .confidence(1.0)
                        .evidenceIds(evidenceIds)
                        .reason(String.format("Required document '%s' is present, intact, and externally verified.", requirement.getName()))
                        .evaluatedAt(LocalDateTime.now())
                        .build();
            }

            if (hasUnavailable) {
                return ComplianceEvaluationResultDto.builder()
                        .requirementId(requirement.getId())
                        .requirementCode(requirement.getRequirementCode())
                        .requirementName(requirement.getName())
                        .requirementType(requirement.getRequirementType())
                        .mandatory(requirement.isMandatory())
                        .status(ComplianceStatus.UNVERIFIED)
                        .confidence(0.0)
                        .evidenceIds(evidenceIds)
                        .reason(String.format("Required document '%s' is present, but external registry verification is unavailable.", requirement.getName()))
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
                .status(ComplianceStatus.COMPLIANT)
                .confidence(1.0)
                .reason(String.format("Required document '%s' (type: %s) is present and verified intact.", requirement.getName(), targetDocType))
                .evaluatedAt(LocalDateTime.now())
                .build();
    }

    private DocumentType determineTargetDocumentType(ComplianceRequirement req) {
        if (req.getExpectedDocumentType() != null) {
            return req.getExpectedDocumentType();
        }
        String code = req.getRequirementCode() != null ? req.getRequirementCode().toUpperCase() : "";
        String name = req.getName() != null ? req.getName().toUpperCase() : "";
        if (code.contains("OEM") || name.contains("OEM")) {
            return DocumentType.OEM_AUTHORIZATION;
        }
        if (code.contains("MII") || name.contains("MAKE IN INDIA") || code.contains("LOCAL_CONTENT")) {
            return DocumentType.MAKE_IN_INDIA_DOCUMENT;
        }
        if (code.contains("STARTUP") || name.contains("STARTUP")) {
            return DocumentType.STARTUP_CERTIFICATE;
        }
        if (code.contains("NSIC") || name.contains("NSIC")) {
            return DocumentType.NSIC_CERTIFICATE;
        }
        if (code.contains("DIGILOCKER") || name.contains("DIGILOCKER")) {
            return DocumentType.DIGILOCKER_DOCUMENT;
        }
        return DocumentType.OTHER;
    }

}
