package com.gem.evidencegraph.temporal;

import com.gem.evidencegraph.entity.EvidenceType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.temporal.rule.DateConsistencyRule;
import com.gem.evidencegraph.temporal.rule.ExpirationRule;
import com.gem.evidencegraph.temporal.rule.FutureValidityRule;
import com.gem.evidencegraph.temporal.rule.IssueAfterBidRule;
import com.gem.evidencegraph.temporal.rule.IssueAfterTenderPublicationRule;
import com.gem.evidencegraph.temporal.rule.MissingTemporalDataRule;
import com.gem.evidencegraph.temporal.rule.NotApplicableRule;
import com.gem.evidencegraph.temporal.rule.TemporalRule;
import com.gem.evidencegraph.temporal.rule.ValidityWindowRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalRuleEngineTest {

    private TemporalVerificationEngine engine;

    @BeforeEach
    void setUp() {
        List<TemporalRule> rules = List.of(
                new DateConsistencyRule(),
                new MissingTemporalDataRule(),
                new FutureValidityRule(),
                new ExpirationRule(),
                new IssueAfterBidRule(),
                new IssueAfterTenderPublicationRule(),
                new ValidityWindowRule(),
                new NotApplicableRule()
        );
        engine = new TemporalVerificationEngine(rules);
    }

    @Test
    @DisplayName("Test 1 — Valid certificate: validFrom <= bidSubmissionDate <= validUntil")
    void test1_validCertificate() {
        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .bidSubmissionDate(LocalDate.of(2026, 9, 20))
                .validFrom(LocalDate.of(2026, 1, 1))
                .validUntil(LocalDate.of(2026, 12, 31))
                .attribute("ISO_9001_CERTIFICATE")
                .requiresTemporalEvaluation(true)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.VALID_AT_BID_DATE, outcome.getPrimaryStatus());
        assertEquals("Evidence validity window covers the bid submission date.", outcome.getReason());
        assertEquals("ValidityWindowRule", outcome.getRuleApplied());
    }

    @Test
    @DisplayName("Test 2 — Expired certificate: validUntil < bidSubmissionDate")
    void test2_expiredCertificate() {
        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .bidSubmissionDate(LocalDate.of(2026, 9, 20))
                .validUntil(LocalDate.of(2026, 8, 31))
                .attribute("SAFETY_CERTIFICATE")
                .requiresTemporalEvaluation(true)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.EXPIRED_AT_BID_DATE, outcome.getPrimaryStatus());
        assertEquals("Evidence expired before the bid submission date.", outcome.getReason());
        assertEquals("ExpirationRule", outcome.getRuleApplied());
    }

    @Test
    @DisplayName("Test 3 — Future certificate: validFrom > bidSubmissionDate")
    void test3_futureCertificate() {
        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .bidSubmissionDate(LocalDate.of(2026, 9, 20))
                .validFrom(LocalDate.of(2026, 10, 1))
                .attribute("COMPLIANCE_CERTIFICATE")
                .requiresTemporalEvaluation(true)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.NOT_YET_VALID_AT_BID_DATE, outcome.getPrimaryStatus());
        assertEquals("Evidence becomes valid after the bid submission date.", outcome.getReason());
        assertEquals("FutureValidityRule", outcome.getRuleApplied());
    }

    @Test
    @DisplayName("Test 4 — Issued after bid: issueDate > bidSubmissionDate")
    void test4_issuedAfterBid() {
        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .bidSubmissionDate(LocalDate.of(2026, 9, 20))
                .issueDate(LocalDate.of(2026, 9, 25))
                .attribute("OEM_AUTHORIZATION")
                .requiresTemporalEvaluation(true)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.ISSUED_AFTER_BID, outcome.getPrimaryStatus());
        assertEquals("Evidence issue date occurs after the bid submission date.", outcome.getReason());
        assertEquals("IssueAfterBidRule", outcome.getRuleApplied());
    }

    @Test
    @DisplayName("Test 5 — Issued after tender publication: issueDate > tender.publicationDate")
    void test5_issuedAfterTenderPublication() {
        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .tenderPublicationDate(LocalDate.of(2026, 8, 1))
                .bidSubmissionDate(LocalDate.of(2026, 9, 20))
                .issueDate(LocalDate.of(2026, 8, 15))
                .attribute("OEM_AUTHORIZATION")
                .requiresTemporalEvaluation(true)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.ISSUED_AFTER_TENDER_PUBLICATION, outcome.getPrimaryStatus());
        assertEquals("Evidence issue date occurs after the tender publication date.", outcome.getReason());
        assertEquals("IssueAfterTenderPublicationRule", outcome.getRuleApplied());
    }

    @Test
    @DisplayName("Test 6 — Conflicting dates: validFrom > validUntil")
    void test6_conflictingDates() {
        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .bidSubmissionDate(LocalDate.of(2026, 9, 20))
                .validFrom(LocalDate.of(2026, 9, 1))
                .validUntil(LocalDate.of(2026, 8, 1))
                .attribute("CERTIFICATE_OF_INCORPORATION")
                .requiresTemporalEvaluation(true)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.CONFLICTING_DATES, outcome.getPrimaryStatus());
        assertEquals("Temporal fields contain conflicting values.", outcome.getReason());
        assertEquals("DateConsistencyRule", outcome.getRuleApplied());
    }

    @Test
    @DisplayName("Test 6b — Conflicting dates: explicit contradiction detected between authoritative sources")
    void test6b_conflictingDatesFlag() {
        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .bidSubmissionDate(LocalDate.of(2026, 9, 20))
                .validFrom(LocalDate.of(2026, 1, 1))
                .validUntil(LocalDate.of(2026, 12, 31))
                .conflictingDatesDetected(true)
                .attribute("TAX_CLEARANCE")
                .requiresTemporalEvaluation(true)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.CONFLICTING_DATES, outcome.getPrimaryStatus());
        assertEquals("Temporal fields contain conflicting values.", outcome.getReason());
        assertEquals("DateConsistencyRule", outcome.getRuleApplied());
    }

    @Test
    @DisplayName("Test 7 — Missing dates: required temporal information unavailable")
    void test7_missingDates() {
        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .bidSubmissionDate(LocalDate.of(2026, 9, 20))
                .attribute("OEM_AUTHORIZATION")
                .requiresTemporalEvaluation(true)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.MISSING_TEMPORAL_DATA, outcome.getPrimaryStatus());
        assertEquals("Required temporal information is unavailable.", outcome.getReason());
        assertEquals("MissingTemporalDataRule", outcome.getRuleApplied());
    }

    @Test
    @DisplayName("Test 8 — Not applicable: evidence does not require temporal evaluation")
    void test8_notApplicable() {
        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .bidSubmissionDate(LocalDate.of(2026, 9, 20))
                .attribute("PAN")
                .requiresTemporalEvaluation(false)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.NOT_APPLICABLE, outcome.getPrimaryStatus());
        assertEquals("Evidence does not require temporal evaluation.", outcome.getReason());
        assertEquals("NotApplicableRule", outcome.getRuleApplied());
    }

    @Test
    @DisplayName("Test 8b — Not applicable auto-detection on static identity attribute")
    void test8b_notApplicableAutoDetect() {
        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .bidSubmissionDate(LocalDate.of(2026, 9, 20))
                .subject("BIDDER")
                .attribute("LEGAL_NAME")
                .sourceType(SourceType.PAN)
                .evidenceType(EvidenceType.REGISTRY_RECORD)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.NOT_APPLICABLE, outcome.getPrimaryStatus());
        assertEquals("Evidence does not require temporal evaluation.", outcome.getReason());
    }

    @Test
    @DisplayName("Test 9 — Boundary condition: validFrom == bidSubmissionDate (start boundary)")
    void test9_startBoundaryValid() {
        LocalDate bidDate = LocalDate.of(2026, 9, 20);
        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .bidSubmissionDate(bidDate)
                .validFrom(bidDate)
                .validUntil(LocalDate.of(2026, 12, 31))
                .attribute("EXPERIENCE_CERTIFICATE")
                .requiresTemporalEvaluation(true)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.VALID_AT_BID_DATE, outcome.getPrimaryStatus());
        assertEquals("Evidence validity window covers the bid submission date.", outcome.getReason());
    }

    @Test
    @DisplayName("Test 9b — Boundary condition: validUntil == bidSubmissionDate (end boundary)")
    void test9b_endBoundaryValid() {
        LocalDate bidDate = LocalDate.of(2026, 9, 20);
        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .bidSubmissionDate(bidDate)
                .validFrom(LocalDate.of(2026, 1, 1))
                .validUntil(bidDate)
                .attribute("GST_REGISTRATION")
                .requiresTemporalEvaluation(true)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.VALID_AT_BID_DATE, outcome.getPrimaryStatus());
        assertEquals("Evidence validity window covers the bid submission date.", outcome.getReason());
    }

    @Test
    @DisplayName("Test 10 — Missing bid submission date: MISSING_TEMPORAL_DATA")
    void test10_missingBidSubmissionDate() {
        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .bidSubmissionDate((LocalDate) null)
                .validFrom(LocalDate.of(2026, 1, 1))
                .validUntil(LocalDate.of(2026, 12, 31))
                .attribute("ISO_CERTIFICATE")
                .requiresTemporalEvaluation(true)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.MISSING_TEMPORAL_DATA, outcome.getPrimaryStatus());
        assertEquals("Required temporal information is unavailable.", outcome.getReason());
    }

    @Test
    @DisplayName("Verify Priority Precedence: CONFLICTING_DATES takes precedence over EXPIRED_AT_BID_DATE")
    void testPriority_conflictingOverExpired() {

        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .bidSubmissionDate(LocalDate.of(2026, 9, 20))
                .validFrom(LocalDate.of(2026, 10, 1))
                .validUntil(LocalDate.of(2026, 8, 1))
                .requiresTemporalEvaluation(true)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.CONFLICTING_DATES, outcome.getPrimaryStatus());

        assertFalse(outcome.getAllFindings().isEmpty());
        assertTrue(outcome.getAllFindings().stream().anyMatch(f -> f.getStatus() == TemporalStatus.EXPIRED_AT_BID_DATE));
    }

    @Test
    @DisplayName("Verify Priority Precedence: NOT_YET_VALID takes precedence over EXPIRED")
    void testPriority_notYetValidPrecedence() {
        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .bidSubmissionDate(LocalDate.of(2026, 9, 20))
                .validFrom(LocalDate.of(2026, 10, 1))
                .validUntil(LocalDate.of(2026, 11, 1))
                .requiresTemporalEvaluation(true)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.NOT_YET_VALID_AT_BID_DATE, outcome.getPrimaryStatus());
    }

    @Test
    @DisplayName("Verify preservation of multiple findings without losing auditability")
    void testPreservationOfMultipleFindings() {

        TemporalEvaluationContext context = TemporalEvaluationContext.builder()
                .bidId(UUID.randomUUID())
                .tenderPublicationDate(LocalDate.of(2026, 8, 1))
                .bidSubmissionDate(LocalDate.of(2026, 9, 20))
                .issueDate(LocalDate.of(2026, 8, 15))
                .validFrom(LocalDate.of(2026, 8, 15))
                .validUntil(LocalDate.of(2027, 8, 15))
                .requiresTemporalEvaluation(true)
                .build();

        TemporalEvaluationOutcome outcome = engine.evaluate(context);

        assertEquals(TemporalStatus.ISSUED_AFTER_TENDER_PUBLICATION, outcome.getPrimaryStatus());
        assertNotNull(outcome.getAllFindings());
        assertTrue(outcome.getAllFindings().stream().anyMatch(f -> f.getStatus() == TemporalStatus.VALID_AT_BID_DATE));
    }

}
