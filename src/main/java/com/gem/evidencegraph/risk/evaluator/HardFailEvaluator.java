package com.gem.evidencegraph.risk.evaluator;

import com.gem.evidencegraph.compliance.ComplianceStatus;
import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.IntegrityStatus;
import com.gem.evidencegraph.entity.RiskFinding;
import com.gem.evidencegraph.risk.RiskSeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class HardFailEvaluator {

    public HardFailResult evaluate(RiskEvaluationContext context, List<RiskFinding> allFindings) {
        List<String> reasons = new ArrayList<>();
        List<RiskFinding> hardFailFindings = new ArrayList<>();

        BidComplianceResultDto complianceResult = context.getComplianceResult();
        Set<DocumentType> mandatoryDocTypes = new HashSet<>();

        if (complianceResult != null && complianceResult.getRequirementResults() != null) {
            for (ComplianceEvaluationResultDto req : complianceResult.getRequirementResults()) {
                if (req.isMandatory()) {
                    if (req.getStatus() == ComplianceStatus.NON_COMPLIANT) {
                        String msg = String.format("[HF-001] Mandatory requirement '%s' (%s) is explicitly NON_COMPLIANT: %s",
                                req.getRequirementName(), req.getRequirementCode(),
                                req.getReason() != null ? req.getReason() : "Failed compliance.");
                        reasons.add(msg);

                        allFindings.stream()
                                .filter(f -> "ELIGIBILITY-001".equals(f.getCode())
                                        && (req.getRequirementCode().equals(f.getSourceId())
                                        || (req.getRequirementId() != null && req.getRequirementId().toString().equals(f.getSourceId()))))
                                .findFirst()
                                .ifPresent(hardFailFindings::add);
                    }

                    DocumentType docType = determineDocumentType(req.getRequirementCode(), req.getRequirementName());
                    if (docType != null) {
                        mandatoryDocTypes.add(docType);
                    }
                }
            }
        }

        List<Document> documents = context.getDocuments();
        if (documents != null) {
            for (Document doc : documents) {
                if (doc.getIntegrityStatus() == IntegrityStatus.FAILED) {

                    boolean isMandatoryDoc = mandatoryDocTypes.contains(doc.getDocumentType())
                            || mandatoryDocTypes.contains(DocumentType.OTHER)
                            || isCommonMandatoryDocument(doc.getDocumentType());

                    if (isMandatoryDoc) {
                        String msg = String.format("[HF-002] Mandatory document '%s' (%s) failed integrity verification.",
                                doc.getOriginalFileName(), doc.getDocumentType());
                        reasons.add(msg);

                        allFindings.stream()
                                .filter(f -> "DOC-INTEGRITY-004".equals(f.getCode())
                                        && (doc.getId() != null && doc.getId().toString().equals(f.getSourceId())))
                                .findFirst()
                                .ifPresent(hardFailFindings::add);
                    }
                }
            }
        }

        for (RiskFinding finding : allFindings) {
            if (finding.getSeverity() == RiskSeverity.CRITICAL && !hardFailFindings.contains(finding)) {
                String msg = String.format("[HF-003] Explicit domain model critical veto triggered by %s: %s",
                        finding.getCode(), finding.getTitle());
                reasons.add(msg);
                hardFailFindings.add(finding);
            }
        }

        boolean isHardFail = !reasons.isEmpty();
        return HardFailResult.builder()
                .hardFail(isHardFail)
                .reasons(reasons)
                .hardFailFindings(hardFailFindings)
                .build();
    }

    private DocumentType determineDocumentType(String code, String name) {
        String c = code != null ? code.toUpperCase() : "";
        String n = name != null ? name.toUpperCase() : "";
        if (c.contains("OEM") || n.contains("OEM")) {
            return DocumentType.OEM_AUTHORIZATION;
        }
        if (c.contains("PAN") || n.contains("PAN")) {
            return DocumentType.PAN_DOCUMENT;
        }
        if (c.contains("GST") || n.contains("GST")) {
            return DocumentType.GST_CERTIFICATE;
        }
        if (c.contains("UDYAM") || n.contains("UDYAM") || c.contains("MSME") || n.contains("MSME")) {
            return DocumentType.UDYAM_CERTIFICATE;
        }
        if (c.contains("MII") || n.contains("MAKE IN INDIA") || c.contains("LOCAL_CONTENT")) {
            return DocumentType.MAKE_IN_INDIA_DOCUMENT;
        }
        if (c.contains("STARTUP") || n.contains("STARTUP")) {
            return DocumentType.STARTUP_CERTIFICATE;
        }
        if (c.contains("NSIC") || n.contains("NSIC")) {
            return DocumentType.NSIC_CERTIFICATE;
        }
        return null;
    }

    private boolean isCommonMandatoryDocument(DocumentType docType) {
        if (docType == null) {
            return false;
        }
        return docType == DocumentType.PAN_DOCUMENT
                || docType == DocumentType.GST_CERTIFICATE
                || docType == DocumentType.OEM_AUTHORIZATION;
    }

}
