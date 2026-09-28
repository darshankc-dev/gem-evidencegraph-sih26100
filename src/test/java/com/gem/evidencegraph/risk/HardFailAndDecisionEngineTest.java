package com.gem.evidencegraph.risk;

import com.gem.evidencegraph.compliance.ComplianceStatus;
import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.IntegrityStatus;
import com.gem.evidencegraph.entity.RiskFinding;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.VerificationStatus;
import com.gem.evidencegraph.risk.evaluator.DecisionEngine;
import com.gem.evidencegraph.risk.evaluator.DecisionOutcome;
import com.gem.evidencegraph.risk.evaluator.HardFailEvaluator;
import com.gem.evidencegraph.risk.evaluator.HardFailResult;
import com.gem.evidencegraph.risk.evaluator.RiskEvaluationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HardFailAndDecisionEngineTest {

    private HardFailEvaluator hardFailEvaluator;
    private DecisionEngine decisionEngine;
    private Bid mockBid;

    @BeforeEach
    void setUp() {
        hardFailEvaluator = new HardFailEvaluator();
        decisionEngine = new DecisionEngine();
        mockBid = Bid.builder().id(UUID.randomUUID()).build();
    }

    @Test
    @DisplayName("Precedence 1: Insufficient evidence -> INSUFFICIENT_EVIDENCE")
    void testInsufficientEvidencePrecedence() {

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .documents(Collections.emptyList())
                .evidences(Collections.emptyList())
                .build();

        HardFailResult hf = HardFailResult.builder().hardFail(false).build();
        DecisionOutcome outcome = decisionEngine.evaluate(ctx, hf, Collections.emptyList(), 0, RiskLevel.LOW);

        assertEquals(DecisionRecommendation.INSUFFICIENT_EVIDENCE, outcome.getRecommendation());
        assertTrue(outcome.getReason().contains("Insufficient evidence"));
    }

    @Test
    @DisplayName("Precedence 2: HF-001 Mandatory NON_COMPLIANT triggers hard-fail and HIGH_RISK recommendation")
    void testRuleHF001MandatoryNonCompliant() {
        ComplianceEvaluationResultDto req = ComplianceEvaluationResultDto.builder()
                .requirementId(UUID.randomUUID())
                .requirementCode("PAN_VALID")
                .requirementName("PAN Validity")
                .mandatory(true)
                .status(ComplianceStatus.NON_COMPLIANT)
                .reason("PAN is cancelled")
                .build();

        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .originalFileName("doc.pdf")
                .documentType(DocumentType.OTHER)
                .integrityStatus(IntegrityStatus.VALID)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .documents(List.of(doc))
                .complianceResult(BidComplianceResultDto.builder()
                        .requirementResults(List.of(req))
                        .build())
                .build();

        RiskFinding finding = RiskFinding.builder()
                .dimension(RiskDimension.ELIGIBILITY)
                .severity(RiskSeverity.CRITICAL)
                .code("ELIGIBILITY-001")
                .sourceId("PAN_VALID")
                .weight(100)
                .build();

        HardFailResult hf = hardFailEvaluator.evaluate(ctx, List.of(finding));
        assertTrue(hf.isHardFail());
        assertTrue(hf.getReasons().get(0).contains("[HF-001]"));

        DecisionOutcome outcome = decisionEngine.evaluate(ctx, hf, List.of(finding), 100, RiskLevel.HIGH);
        assertEquals(DecisionRecommendation.HIGH_RISK, outcome.getRecommendation());
        assertTrue(outcome.getReason().contains("Hard-fail condition detected"));
    }

    @Test
    @DisplayName("Precedence 2: HF-002 Mandatory document with IntegrityStatus.FAILED triggers hard-fail")
    void testRuleHF002MandatoryDocumentFailed() {
        ComplianceEvaluationResultDto req = ComplianceEvaluationResultDto.builder()
                .requirementId(UUID.randomUUID())
                .requirementCode("GST_REQ")
                .requirementName("GST Registration")
                .mandatory(true)
                .status(ComplianceStatus.COMPLIANT)
                .build();

        UUID docId = UUID.randomUUID();
        Document doc = Document.builder()
                .id(docId)
                .originalFileName("gst_cert.pdf")
                .documentType(DocumentType.GST_CERTIFICATE)
                .integrityStatus(IntegrityStatus.FAILED)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .documents(List.of(doc))
                .complianceResult(BidComplianceResultDto.builder()
                        .requirementResults(List.of(req))
                        .build())
                .build();

        RiskFinding finding = RiskFinding.builder()
                .dimension(RiskDimension.DOCUMENT_INTEGRITY)
                .severity(RiskSeverity.CRITICAL)
                .code("DOC-INTEGRITY-004")
                .sourceId(docId.toString())
                .weight(100)
                .build();

        HardFailResult hf = hardFailEvaluator.evaluate(ctx, List.of(finding));
        assertTrue(hf.isHardFail());
        assertTrue(hf.getReasons().get(0).contains("[HF-002]"));

        DecisionOutcome outcome = decisionEngine.evaluate(ctx, hf, List.of(finding), 100, RiskLevel.HIGH);
        assertEquals(DecisionRecommendation.HIGH_RISK, outcome.getRecommendation());
    }

    @Test
    @DisplayName("Precedence 3: High numerical score WITHOUT hard fail does NOT automatically disqualify (yields REVIEW)")
    void testHighScoreWithoutHardFailYieldsReview() {

        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .originalFileName("doc.pdf")
                .documentType(DocumentType.OTHER)
                .integrityStatus(IntegrityStatus.VALID)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .documents(List.of(doc))
                .build();

        RiskFinding finding = RiskFinding.builder()
                .dimension(RiskDimension.ELIGIBILITY)
                .severity(RiskSeverity.HIGH)
                .code("ELIGIBILITY-002")
                .title("Non-mandatory requirement non-compliant")
                .weight(50)
                .build();

        HardFailResult hf = hardFailEvaluator.evaluate(ctx, List.of(finding));
        assertFalse(hf.isHardFail());

        DecisionOutcome outcome = decisionEngine.evaluate(ctx, hf, List.of(finding), 50, RiskLevel.MEDIUM);
        assertEquals(DecisionRecommendation.REVIEW, outcome.getRecommendation());
        assertFalse(outcome.getReason().toLowerCase().contains("disqualified"));
    }

    @Test
    @DisplayName("Precedence 3: Review triggers (source unavailable, unverified, temporal concern) yield REVIEW")
    void testReviewTriggersYieldReview() {
        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .originalFileName("doc.pdf")
                .integrityStatus(IntegrityStatus.VALID)
                .build();

        Evidence ev = Evidence.builder()
                .id(UUID.randomUUID())
                .sourceType(SourceType.GST)
                .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .documents(List.of(doc))
                .evidences(List.of(ev))
                .build();

        RiskFinding finding = RiskFinding.builder()
                .dimension(RiskDimension.VERIFICATION)
                .severity(RiskSeverity.MEDIUM)
                .code("VERIFICATION-002")
                .title("Authoritative verification source unavailable")
                .weight(25)
                .build();

        HardFailResult hf = hardFailEvaluator.evaluate(ctx, List.of(finding));
        assertFalse(hf.isHardFail());

        DecisionOutcome outcome = decisionEngine.evaluate(ctx, hf, List.of(finding), 25, RiskLevel.MEDIUM);
        assertEquals(DecisionRecommendation.REVIEW, outcome.getRecommendation());
        assertTrue(outcome.getReason().contains("External verification source unavailable"));
    }

    @Test
    @DisplayName("Precedence 4: Sufficient evidence, no hard fail, and no review triggers yields RECOMMEND")
    void testCleanEvaluationYieldsRecommend() {
        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .originalFileName("doc.pdf")
                .integrityStatus(IntegrityStatus.VALID)
                .build();

        Evidence ev = Evidence.builder()
                .id(UUID.randomUUID())
                .sourceType(SourceType.GST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .documents(List.of(doc))
                .evidences(List.of(ev))
                .build();

        RiskFinding f1 = RiskFinding.builder()
                .dimension(RiskDimension.DOCUMENT_INTEGRITY)
                .severity(RiskSeverity.INFO)
                .code("DOC-INTEGRITY-000")
                .weight(0)
                .build();

        RiskFinding f2 = RiskFinding.builder()
                .dimension(RiskDimension.VERIFICATION)
                .severity(RiskSeverity.INFO)
                .code("VERIFICATION-000")
                .weight(0)
                .build();

        HardFailResult hf = hardFailEvaluator.evaluate(ctx, List.of(f1, f2));
        assertFalse(hf.isHardFail());

        DecisionOutcome outcome = decisionEngine.evaluate(ctx, hf, List.of(f1, f2), 0, RiskLevel.LOW);
        assertEquals(DecisionRecommendation.RECOMMEND, outcome.getRecommendation());
        assertTrue(outcome.getReason().contains("Applicable deterministic checks have been satisfied"));
    }

}
