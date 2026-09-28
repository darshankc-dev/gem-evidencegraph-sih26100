package com.gem.evidencegraph.risk;

import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.IntegrityStatus;
import com.gem.evidencegraph.entity.RiskFinding;
import com.gem.evidencegraph.risk.evaluator.DimensionEvaluationResult;
import com.gem.evidencegraph.risk.evaluator.DocumentIntegrityRiskEvaluator;
import com.gem.evidencegraph.risk.evaluator.RiskEvaluationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentIntegrityRiskEvaluatorTest {

    private DocumentIntegrityRiskEvaluator evaluator;
    private Bid mockBid;

    @BeforeEach
    void setUp() {
        evaluator = new DocumentIntegrityRiskEvaluator();
        mockBid = Bid.builder().id(UUID.randomUUID()).build();
    }

    @Test
    @DisplayName("Document VALID maps to INFO (0 weight)")
    void testValidDocument() {
        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .originalFileName("gst_cert.pdf")
                .documentType(DocumentType.GST_CERTIFICATE)
                .integrityStatus(IntegrityStatus.VALID)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .documents(List.of(doc))
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(0, result.getRiskScore());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.INFO, finding.getSeverity());
        assertEquals(0, finding.getWeight());
        assertEquals("DOC-INTEGRITY-000", finding.getCode());
        assertNotNull(finding.getSourceId());
        assertTrue(finding.getRule().startsWith("RULE-"));
    }

    @Test
    @DisplayName("Document NOT_CHECKED maps to LOW (10 weight)")
    void testNotCheckedDocument() {
        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .originalFileName("pending.pdf")
                .documentType(DocumentType.OTHER)
                .integrityStatus(IntegrityStatus.NOT_CHECKED)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .documents(List.of(doc))
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(10, result.getRiskScore());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.LOW, finding.getSeverity());
        assertEquals(10, finding.getWeight());
        assertEquals("DOC-INTEGRITY-001", finding.getCode());
    }

    @Test
    @DisplayName("Document REQUIRES_REVIEW maps to MEDIUM (25 weight)")
    void testRequiresReviewDocument() {
        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .originalFileName("review.pdf")
                .documentType(DocumentType.PAN_DOCUMENT)
                .integrityStatus(IntegrityStatus.REQUIRES_REVIEW)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .documents(List.of(doc))
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(25, result.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.MEDIUM, finding.getSeverity());
        assertEquals(25, finding.getWeight());
        assertEquals("DOC-INTEGRITY-002", finding.getCode());
    }

    @Test
    @DisplayName("Document SUSPICIOUS maps to HIGH (50 weight)")
    void testSuspiciousDocument() {
        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .originalFileName("altered.pdf")
                .documentType(DocumentType.OEM_AUTHORIZATION)
                .integrityStatus(IntegrityStatus.SUSPICIOUS)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .documents(List.of(doc))
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(50, result.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.HIGH, finding.getSeverity());
        assertEquals(50, finding.getWeight());
        assertEquals("DOC-INTEGRITY-003", finding.getCode());
    }

    @Test
    @DisplayName("Document FAILED maps to CRITICAL (100 weight)")
    void testFailedDocument() {
        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .originalFileName("corrupt.pdf")
                .documentType(DocumentType.GST_CERTIFICATE)
                .integrityStatus(IntegrityStatus.FAILED)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .documents(List.of(doc))
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(100, result.getRiskScore());
        assertEquals(RiskLevel.HIGH, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.CRITICAL, finding.getSeverity());
        assertEquals(100, finding.getWeight());
        assertEquals("DOC-INTEGRITY-004", finding.getCode());
    }

}
