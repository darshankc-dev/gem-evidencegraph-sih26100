package com.gem.evidencegraph.risk;

import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.RiskFinding;
import com.gem.evidencegraph.risk.evaluator.DimensionEvaluationResult;
import com.gem.evidencegraph.risk.evaluator.RiskEvaluationContext;
import com.gem.evidencegraph.risk.evaluator.TemporalRiskEvaluator;
import com.gem.evidencegraph.temporal.TemporalStatus;
import com.gem.evidencegraph.temporal.dto.BidTemporalEvaluationResultDto;
import com.gem.evidencegraph.temporal.dto.TemporalEvaluationResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalRiskEvaluatorTest {

    private TemporalRiskEvaluator evaluator;
    private Bid mockBid;

    @BeforeEach
    void setUp() {
        evaluator = new TemporalRiskEvaluator();
        mockBid = Bid.builder().id(UUID.randomUUID()).build();
    }

    @ParameterizedTest(name = "TemporalStatus {0} should map to severity {1} with weight {2}")
    @CsvSource({
            "VALID_AT_BID_DATE, INFO, 0",
            "NOT_APPLICABLE, INFO, 0",
            "MISSING_TEMPORAL_DATA, MEDIUM, 25",
            "ISSUED_AFTER_TENDER_PUBLICATION, MEDIUM, 25",
            "ISSUED_AFTER_BID, HIGH, 50",
            "NOT_YET_VALID_AT_BID_DATE, HIGH, 50",
            "EXPIRED_AT_BID_DATE, HIGH, 50",
            "CONFLICTING_DATES, HIGH, 50"
    })
    @DisplayName("Verify deterministic mapping of all 8 Step 8 temporal statuses")
    void testTemporalStatusMapping(TemporalStatus status, RiskSeverity expectedSeverity, int expectedWeight) {
        UUID evidenceId = UUID.randomUUID();
        TemporalEvaluationResponseDto item = TemporalEvaluationResponseDto.builder()
                .evidenceId(evidenceId)
                .status(status)
                .reason("Deterministic reason for " + status)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .temporalResult(BidTemporalEvaluationResultDto.builder()
                        .results(List.of(item))
                        .build())
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
    @DisplayName("Multiple temporal findings: EXPIRED (50) + CONFLICTING_DATES (50) = 100 -> HIGH")
    void testCumulativeTemporalScore() {
        TemporalEvaluationResponseDto item1 = TemporalEvaluationResponseDto.builder()
                .evidenceId(UUID.randomUUID())
                .status(TemporalStatus.EXPIRED_AT_BID_DATE)
                .reason("Expired 2 months before bid")
                .build();

        TemporalEvaluationResponseDto item2 = TemporalEvaluationResponseDto.builder()
                .evidenceId(UUID.randomUUID())
                .status(TemporalStatus.CONFLICTING_DATES)
                .reason("Dates diverge")
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .temporalResult(BidTemporalEvaluationResultDto.builder()
                        .results(List.of(item1, item2))
                        .build())
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(100, result.getRiskScore());
        assertEquals(RiskLevel.HIGH, result.getRiskLevel());
    }

}
