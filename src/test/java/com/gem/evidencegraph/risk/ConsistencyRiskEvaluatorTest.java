package com.gem.evidencegraph.risk;

import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Bidder;
import com.gem.evidencegraph.entity.EntityResolutionResult;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.EvidenceRelationship;
import com.gem.evidencegraph.entity.RelationshipType;
import com.gem.evidencegraph.entity.RiskFinding;
import com.gem.evidencegraph.entityresolution.EntityMatchStatus;
import com.gem.evidencegraph.risk.evaluator.ConsistencyRiskEvaluator;
import com.gem.evidencegraph.risk.evaluator.DimensionEvaluationResult;
import com.gem.evidencegraph.risk.evaluator.RiskEvaluationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsistencyRiskEvaluatorTest {

    private ConsistencyRiskEvaluator evaluator;
    private Bid mockBid;
    private Bidder mockBidder;

    @BeforeEach
    void setUp() {
        evaluator = new ConsistencyRiskEvaluator();
        mockBid = Bid.builder().id(UUID.randomUUID()).build();
        mockBidder = Bidder.builder().id(UUID.randomUUID()).legalName("Acme Corp").build();
    }

    @Test
    @DisplayName("Entity resolution MATCH maps to INFO (0 weight)")
    void testEntityMatch() {
        EntityResolutionResult err = EntityResolutionResult.builder()
                .id(UUID.randomUUID())
                .bidder(mockBidder)
                .matchStatus(EntityMatchStatus.MATCH)
                .confidence(1.0)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .bidder(mockBidder)
                .entityResolutionResults(List.of(err))
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(0, result.getRiskScore());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.INFO, finding.getSeverity());
        assertEquals(0, finding.getWeight());
        assertEquals("CONSISTENCY-000", finding.getCode());
        assertNotNull(finding.getSourceId());
    }

    @Test
    @DisplayName("Entity resolution PROBABLE_MATCH maps to LOW (10 weight)")
    void testEntityProbableMatch() {
        EntityResolutionResult err = EntityResolutionResult.builder()
                .id(UUID.randomUUID())
                .bidder(mockBidder)
                .matchStatus(EntityMatchStatus.PROBABLE_MATCH)
                .confidence(0.8)
                .explanation("Address minor divergence")
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .bidder(mockBidder)
                .entityResolutionResults(List.of(err))
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(10, result.getRiskScore());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.LOW, finding.getSeverity());
        assertEquals(10, finding.getWeight());
        assertEquals("CONSISTENCY-001", finding.getCode());
    }

    @Test
    @DisplayName("Entity resolution INSUFFICIENT_DATA maps to MEDIUM (25 weight)")
    void testEntityInsufficientData() {
        EntityResolutionResult err = EntityResolutionResult.builder()
                .id(UUID.randomUUID())
                .bidder(mockBidder)
                .matchStatus(EntityMatchStatus.INSUFFICIENT_DATA)
                .confidence(0.0)
                .explanation("Missing statutory identifiers")
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .bidder(mockBidder)
                .entityResolutionResults(List.of(err))
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(25, result.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.MEDIUM, finding.getSeverity());
        assertEquals(25, finding.getWeight());
        assertEquals("CONSISTENCY-002", finding.getCode());
    }

    @Test
    @DisplayName("Entity resolution MISMATCH maps to HIGH (50 weight) and uses factual non-fraud phrasing")
    void testEntityMismatch() {
        EntityResolutionResult err = EntityResolutionResult.builder()
                .id(UUID.randomUUID())
                .bidder(mockBidder)
                .matchStatus(EntityMatchStatus.MISMATCH)
                .confidence(0.0)
                .explanation("PAN mismatch between profile and GST")
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .bidder(mockBidder)
                .entityResolutionResults(List.of(err))
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(50, result.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.HIGH, finding.getSeverity());
        assertEquals(50, finding.getWeight());
        assertEquals("CONSISTENCY-003", finding.getCode());
        assertEquals("Conflicting bidder identity information", finding.getTitle());
        assertTrue(finding.getDescription().contains("Conflicting bidder identity information"));

        assertFalse(finding.getDescription().toLowerCase().contains("fraud"));
    }

    @Test
    @DisplayName("Verified contradiction between evidence sources maps to HIGH (50 weight)")
    void testEvidenceContradiction() {
        Evidence ev1 = Evidence.builder().id(UUID.randomUUID()).build();
        Evidence ev2 = Evidence.builder().id(UUID.randomUUID()).build();
        EvidenceRelationship rel = EvidenceRelationship.builder()
                .id(UUID.randomUUID())
                .sourceEvidence(ev1)
                .targetEvidence(ev2)
                .relationshipType(RelationshipType.CONTRADICTS)
                .reason("GST active status contradicts de-registration certificate")
                .confidence(1.0)
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .bidder(mockBidder)
                .evidenceRelationships(List.of(rel))
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(50, result.getRiskScore());
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());

        RiskFinding finding = result.getFindings().get(0);
        assertEquals(RiskSeverity.HIGH, finding.getSeverity());
        assertEquals(50, finding.getWeight());
        assertEquals("CONSISTENCY-004", finding.getCode());
        assertEquals("Conflicting evidence detected", finding.getTitle());
        assertFalse(finding.getDescription().toLowerCase().contains("fraud"));
    }

    @Test
    @DisplayName("Combined findings: MISMATCH (50) + Contradiction (50) = 100 -> HIGH")
    void testCombinedConsistencyFindings() {
        EntityResolutionResult err = EntityResolutionResult.builder()
                .id(UUID.randomUUID())
                .bidder(mockBidder)
                .matchStatus(EntityMatchStatus.MISMATCH)
                .build();

        EvidenceRelationship rel = EvidenceRelationship.builder()
                .id(UUID.randomUUID())
                .relationshipType(RelationshipType.CONTRADICTS)
                .reason("PAN mismatch")
                .build();

        RiskEvaluationContext ctx = RiskEvaluationContext.builder()
                .bid(mockBid)
                .bidder(mockBidder)
                .entityResolutionResults(List.of(err))
                .evidenceRelationships(List.of(rel))
                .build();

        DimensionEvaluationResult result = evaluator.evaluate(ctx);
        assertEquals(100, result.getRiskScore());
        assertEquals(RiskLevel.HIGH, result.getRiskLevel());
        assertEquals(2, result.getFindings().size());
    }

}
