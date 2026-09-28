package com.gem.evidencegraph.risk;

import com.gem.evidencegraph.compliance.ComplianceStatus;
import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.RiskFinding;
import com.gem.evidencegraph.risk.evaluator.DimensionEvaluationResult;
import com.gem.evidencegraph.risk.evaluator.EligibilityRiskEvaluator;
import com.gem.evidencegraph.risk.evaluator.RiskEvaluationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EligibilityRiskEvaluatorTest {

    private EligibilityRiskEvaluator evaluator;
    private Bid mockBid;

    @BeforeEach
    void setUp() {
        evaluator = new EligibilityRiskEvaluator();
        mockBid = Bid.builder().id(UUID.randomUUID()).build();
    }

    @Test
    @DisplayName("Compliant requirement maps to INFO (0 weight)")
    void testCompliantRequirement() {
        ComplianceEvaluationResultDto req = ComplianceEvaluationResultDto.builder()
                .requirementId(UUID.randomUUID())
                .requirementCode("GST_REG")
                .requirementName("GST Registration")
                .mandatory(true)
                .status(ComplianceStatus.COMPLIANT)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .complianceResult(BidComplianceResultDto.builder()
                        .requirementResults(List.of(req))
                        .build())
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(0, result.getRiskScore());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());
        assertEquals(1, result.getFindings().size());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.INFO, finding.getSeverity());
        assertEquals(0, finding.getWeight());
        assertEquals("ELIGIBILITY-000", finding.getCode());
        assertNotNull(finding.getSourceId());
        assertTrue(finding.getRule().startsWith("RULE-"));
    }

    @Test
    @DisplayName("Mandatory non-compliance maps to CRITICAL (100 weight)")
    void testMandatoryNonCompliance() {
        ComplianceEvaluationResultDto req = ComplianceEvaluationResultDto.builder()
                .requirementId(UUID.randomUUID())
                .requirementCode("PAN_VALID")
                .requirementName("PAN Validity")
                .mandatory(true)
                .status(ComplianceStatus.NON_COMPLIANT)
                .reason("PAN is inactive or invalid")
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .complianceResult(BidComplianceResultDto.builder()
                        .requirementResults(List.of(req))
                        .build())
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(100, result.getRiskScore());
        assertEquals(RiskLevel.HIGH, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.CRITICAL, finding.getSeverity());
        assertEquals(100, finding.getWeight());
        assertEquals("ELIGIBILITY-001", finding.getCode());
    }

    @Test
    @DisplayName("Non-mandatory non-compliance maps to HIGH (50 weight)")
    void testNonMandatoryNonCompliance() {
        ComplianceEvaluationResultDto req = ComplianceEvaluationResultDto.builder()
                .requirementId(UUID.randomUUID())
                .requirementCode("OPTIONAL_CERT")
                .requirementName("Optional Green Certificate")
                .mandatory(false)
                .status(ComplianceStatus.NON_COMPLIANT)
                .reason("Certificate expired")
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .complianceResult(BidComplianceResultDto.builder()
                        .requirementResults(List.of(req))
                        .build())
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(50, result.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.HIGH, finding.getSeverity());
        assertEquals(50, finding.getWeight());
        assertEquals("ELIGIBILITY-002", finding.getCode());
    }

    @Test
    @DisplayName("Mandatory missing maps to HIGH (50 weight)")
    void testMandatoryMissing() {
        ComplianceEvaluationResultDto req = ComplianceEvaluationResultDto.builder()
                .requirementId(UUID.randomUUID())
                .requirementCode("OEM_AUTH")
                .requirementName("OEM Authorization")
                .mandatory(true)
                .status(ComplianceStatus.MISSING)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .complianceResult(BidComplianceResultDto.builder()
                        .requirementResults(List.of(req))
                        .build())
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(50, result.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.HIGH, finding.getSeverity());
        assertEquals(50, finding.getWeight());
        assertEquals("ELIGIBILITY-003", finding.getCode());
    }

    @Test
    @DisplayName("Non-mandatory missing maps to MEDIUM (25 weight)")
    void testNonMandatoryMissing() {
        ComplianceEvaluationResultDto req = ComplianceEvaluationResultDto.builder()
                .requirementId(UUID.randomUUID())
                .requirementCode("OPT_DOC")
                .requirementName("Optional Catalog")
                .mandatory(false)
                .status(ComplianceStatus.MISSING)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .complianceResult(BidComplianceResultDto.builder()
                        .requirementResults(List.of(req))
                        .build())
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(25, result.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.MEDIUM, finding.getSeverity());
        assertEquals(25, finding.getWeight());
        assertEquals("ELIGIBILITY-004", finding.getCode());
    }

    @Test
    @DisplayName("Unverified maps to MEDIUM (25 weight)")
    void testUnverified() {
        ComplianceEvaluationResultDto req = ComplianceEvaluationResultDto.builder()
                .requirementId(UUID.randomUUID())
                .requirementCode("UDYAM_REG")
                .requirementName("Udyam Registration")
                .mandatory(true)
                .status(ComplianceStatus.UNVERIFIED)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .complianceResult(BidComplianceResultDto.builder()
                        .requirementResults(List.of(req))
                        .build())
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(25, result.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.MEDIUM, finding.getSeverity());
        assertEquals(25, finding.getWeight());
        assertEquals("ELIGIBILITY-005", finding.getCode());
    }

    @Test
    @DisplayName("Contradictory maps to HIGH (50 weight)")
    void testContradictory() {
        ComplianceEvaluationResultDto req = ComplianceEvaluationResultDto.builder()
                .requirementId(UUID.randomUUID())
                .requirementCode("TURNOVER")
                .requirementName("Annual Turnover")
                .mandatory(true)
                .status(ComplianceStatus.CONTRADICTORY)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .complianceResult(BidComplianceResultDto.builder()
                        .requirementResults(List.of(req))
                        .build())
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(50, result.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.HIGH, finding.getSeverity());
        assertEquals(50, finding.getWeight());
        assertEquals("ELIGIBILITY-006", finding.getCode());
    }

    @Test
    @DisplayName("Pending human review maps to MEDIUM (25 weight)")
    void testPendingHumanReview() {
        ComplianceEvaluationResultDto req = ComplianceEvaluationResultDto.builder()
                .requirementId(UUID.randomUUID())
                .requirementCode("LAND_BORDER")
                .requirementName("Land Border Declaration")
                .mandatory(true)
                .status(ComplianceStatus.PENDING_HUMAN_REVIEW)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .complianceResult(BidComplianceResultDto.builder()
                        .requirementResults(List.of(req))
                        .build())
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(25, result.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.MEDIUM, finding.getSeverity());
        assertEquals(25, finding.getWeight());
        assertEquals("ELIGIBILITY-007", finding.getCode());
    }

    @Test
    @DisplayName("Not applicable maps to INFO (0 weight)")
    void testNotApplicable() {
        ComplianceEvaluationResultDto req = ComplianceEvaluationResultDto.builder()
                .requirementId(UUID.randomUUID())
                .requirementCode("STARTUP_EXEMPT")
                .requirementName("Startup Exemption")
                .mandatory(false)
                .status(ComplianceStatus.NOT_APPLICABLE)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .complianceResult(BidComplianceResultDto.builder()
                        .requirementResults(List.of(req))
                        .build())
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(0, result.getRiskScore());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.INFO, finding.getSeverity());
        assertEquals(0, finding.getWeight());
        assertEquals("ELIGIBILITY-008", finding.getCode());
    }

}
