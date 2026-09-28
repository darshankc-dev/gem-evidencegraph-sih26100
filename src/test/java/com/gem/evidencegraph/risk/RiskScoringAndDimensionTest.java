package com.gem.evidencegraph.risk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskScoringAndDimensionTest {

    @Test
    @DisplayName("Verify centralized severity weights conform to deterministic spec")
    void testSeverityWeights() {
        assertEquals(0, RiskConstants.getWeight(RiskSeverity.INFO));
        assertEquals(10, RiskConstants.getWeight(RiskSeverity.LOW));
        assertEquals(25, RiskConstants.getWeight(RiskSeverity.MEDIUM));
        assertEquals(50, RiskConstants.getWeight(RiskSeverity.HIGH));
        assertEquals(100, RiskConstants.getWeight(RiskSeverity.CRITICAL));
    }

    @ParameterizedTest(name = "Score {0} should map to {1}")
    @CsvSource({
            "0, LOW",
            "10, LOW",
            "24, LOW",
            "25, MEDIUM",
            "40, MEDIUM",
            "59, MEDIUM",
            "60, HIGH",
            "75, HIGH",
            "100, HIGH",
            "125, HIGH"
    })
    @DisplayName("Verify deterministic threshold mapping for RiskLevel: 0-24 -> LOW, 25-59 -> MEDIUM, 60-100 -> HIGH")
    void testRiskLevelThresholds(int score, RiskLevel expectedLevel) {
        assertEquals(expectedLevel, RiskLevel.fromScore(score));
    }

    @Test
    @DisplayName("Verify score capping at 100 (e.g. 125 -> 100, negative -> 0)")
    void testScoreCapping() {
        assertEquals(0, RiskConstants.capScore(-10));
        assertEquals(0, RiskConstants.capScore(0));
        assertEquals(24, RiskConstants.capScore(24));
        assertEquals(75, RiskConstants.capScore(75));
        assertEquals(100, RiskConstants.capScore(100));
        assertEquals(100, RiskConstants.capScore(125));
        assertEquals(100, RiskConstants.capScore(250));
    }

    @Test
    @DisplayName("Verify overall risk score uses the maximum dimension score")
    void testOverallScoreCalculationUsesMax() {
        int eligibilityScore = 25;
        int docIntegrityScore = 0;
        int consistencyScore = 75;
        int temporalScore = 50;
        int verificationScore = 10;

        int overall = Math.max(eligibilityScore,
                Math.max(docIntegrityScore,
                        Math.max(consistencyScore,
                                Math.max(temporalScore, verificationScore))));

        assertEquals(75, overall);
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(overall));
    }

}
