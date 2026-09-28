package com.gem.evidencegraph.risk;

import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.RiskFinding;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.VerificationStatus;
import com.gem.evidencegraph.risk.evaluator.DimensionEvaluationResult;
import com.gem.evidencegraph.risk.evaluator.RiskEvaluationContext;
import com.gem.evidencegraph.risk.evaluator.VerificationRiskEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationRiskEvaluatorTest {

    private VerificationRiskEvaluator evaluator;
    private Bid mockBid;

    @BeforeEach
    void setUp() {
        evaluator = new VerificationRiskEvaluator();
        mockBid = Bid.builder().id(UUID.randomUUID()).build();
    }

    @ParameterizedTest(name = "VerificationStatus {0} should map to severity {1} with weight {2}")
    @CsvSource({
            "VERIFIED, INFO, 0",
            "UNVERIFIED, MEDIUM, 25",
            "SOURCE_UNAVAILABLE, MEDIUM, 25",
            "PENDING_HUMAN_REVIEW, MEDIUM, 25",
            "MISMATCH, HIGH, 50",
            "NOT_APPLICABLE, INFO, 0"
    })
    @DisplayName("Verify deterministic mapping of VerificationStatus")
    void testVerificationStatusMapping(VerificationStatus status, RiskSeverity expectedSeverity, int expectedWeight) {
        UUID evidenceId = UUID.randomUUID();
        Evidence ev = Evidence.builder()
                .id(evidenceId)
                .sourceType(SourceType.GST)
                .attribute("GSTIN")
                .subject("29ABCDE1234F1Z5")
                .verificationStatus(status)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .evidences(List.of(ev))
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(expectedWeight, result.getRiskScore());
        assertEquals(1, result.getFindings().size());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(expectedSeverity, finding.getSeverity());
        assertEquals(expectedWeight, finding.getWeight());
        assertEquals(evidenceId.toString(), finding.getSourceId());
        assertNotNull(finding.getCode());
        assertTrue(finding.getRule().startsWith("RULE-"));
    }

    @Test
    @DisplayName("SOURCE_UNAVAILABLE clearly explains verification could not be completed and does not indicate bidder failure")
    void testSourceUnavailableFactualWording() {
        Evidence ev = Evidence.builder()
                .id(UUID.randomUUID())
                .sourceType(SourceType.PAN)
                .attribute("PAN")
                .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .evidences(List.of(ev))
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        RiskFinding finding = result.getFindings().get(0);

        assertEquals("VERIFICATION-002", finding.getCode());
        assertTrue(finding.getDescription().contains("temporarily unavailable"));
        assertTrue(finding.getDescription().contains("does not indicate bidder non-compliance"));
        assertFalse(finding.getDescription().toLowerCase().contains("fraud"));
    }

}
