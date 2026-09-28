package com.gem.evidencegraph.risk.evaluator;

import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.IntegrityStatus;
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
public class DocumentIntegrityRiskEvaluator {

    public DimensionEvaluationResult evaluate(RiskEvaluationContext context) {
        List<RiskFinding> findings = new ArrayList<>();
        List<Document> documents = context.getDocuments();

        if (documents != null) {
            for (Document doc : documents) {
                IntegrityStatus status = doc.getIntegrityStatus();
                if (status == null) {
                    status = IntegrityStatus.NOT_CHECKED;
                }

                RiskSeverity severity;
                String code;
                String title;
                String description;

                switch (status) {
                    case VALID -> {
                        severity = RiskSeverity.INFO;
                        code = "DOC-INTEGRITY-000";
                        title = "Document integrity verified";
                        description = String.format("Document '%s' (%s) integrity and cryptographic hash are valid.",
                                doc.getOriginalFileName(), doc.getDocumentType());
                    }
                    case NOT_CHECKED -> {
                        severity = RiskSeverity.LOW;
                        code = "DOC-INTEGRITY-001";
                        title = "Document integrity not checked";
                        description = String.format("Document '%s' (%s) integrity has not yet been validated.",
                                doc.getOriginalFileName(), doc.getDocumentType());
                    }
                    case REQUIRES_REVIEW -> {
                        severity = RiskSeverity.MEDIUM;
                        code = "DOC-INTEGRITY-002";
                        title = "Document integrity requires review";
                        description = String.format("Document '%s' (%s) requires manual integrity review.",
                                doc.getOriginalFileName(), doc.getDocumentType());
                    }
                    case SUSPICIOUS -> {
                        severity = RiskSeverity.HIGH;
                        code = "DOC-INTEGRITY-003";
                        title = "Document integrity suspicious";
                        description = String.format("Document '%s' (%s) exhibits structural or checksum anomalies.",
                                doc.getOriginalFileName(), doc.getDocumentType());
                    }
                    case FAILED -> {
                        severity = RiskSeverity.CRITICAL;
                        code = "DOC-INTEGRITY-004";
                        title = "Document integrity check failed";
                        description = String.format("Document '%s' (%s) failed cryptographic checksum or format validation.",
                                doc.getOriginalFileName(), doc.getDocumentType());
                    }
                    default -> {
                        severity = RiskSeverity.LOW;
                        code = "DOC-INTEGRITY-099";
                        title = "Document status " + status;
                        description = String.format("Document '%s' has status: %s", doc.getOriginalFileName(), status);
                    }
                }

                int weight = RiskConstants.getWeight(severity);
                String sourceId = doc.getId() != null ? doc.getId().toString() : doc.getOriginalFileName();

                RiskFinding finding = RiskFinding.builder()
                        .bid(context.getBid())
                        .dimension(RiskDimension.DOCUMENT_INTEGRITY)
                        .severity(severity)
                        .code(code)
                        .title(title)
                        .description(description)
                        .sourceType("DOCUMENT")
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

        String summary = String.format("Document integrity evaluated with score %d (%s) across %d document(s).",
                cappedScore, level, documents != null ? documents.size() : 0);
        String reason = cappedScore == 0
                ? "All submitted documents possess valid cryptographic integrity."
                : String.format("Document integrity risk score of %d derived from %d finding(s).", cappedScore, findings.size());

        return DimensionEvaluationResult.builder()
                .dimension(RiskDimension.DOCUMENT_INTEGRITY)
                .riskScore(cappedScore)
                .riskLevel(level)
                .findingCount(findings.size())
                .summary(summary)
                .reason(reason)
                .findings(findings)
                .build();
    }

}
