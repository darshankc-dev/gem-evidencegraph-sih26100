package com.gem.evidencegraph.risk;

import com.gem.evidencegraph.compliance.ComplianceService;
import com.gem.evidencegraph.compliance.ComplianceStatus;
import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.BidRiskAssessment;
import com.gem.evidencegraph.entity.BidStatus;
import com.gem.evidencegraph.entity.Bidder;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.IntegrityStatus;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.Tender;
import com.gem.evidencegraph.entity.TenderStatus;
import com.gem.evidencegraph.entity.VerificationStatus;
import com.gem.evidencegraph.entityresolution.EntityResolutionService;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.BidRiskAssessmentRepository;
import com.gem.evidencegraph.repository.ClaimRepository;
import com.gem.evidencegraph.repository.DocumentRepository;
import com.gem.evidencegraph.repository.EntityResolutionResultRepository;
import com.gem.evidencegraph.repository.EvidenceRelationshipRepository;
import com.gem.evidencegraph.repository.EvidenceRepository;
import com.gem.evidencegraph.risk.dto.BidRiskAssessmentResponseDto;
import com.gem.evidencegraph.risk.dto.RiskDimensionResultDto;
import com.gem.evidencegraph.risk.dto.RiskFindingDto;
import com.gem.evidencegraph.risk.evaluator.ConsistencyRiskEvaluator;
import com.gem.evidencegraph.risk.evaluator.DecisionEngine;
import com.gem.evidencegraph.risk.evaluator.DocumentIntegrityRiskEvaluator;
import com.gem.evidencegraph.risk.evaluator.EligibilityRiskEvaluator;
import com.gem.evidencegraph.risk.evaluator.HardFailEvaluator;
import com.gem.evidencegraph.risk.evaluator.RiskAssessmentEngine;
import com.gem.evidencegraph.risk.evaluator.TemporalRiskEvaluator;
import com.gem.evidencegraph.risk.evaluator.VerificationRiskEvaluator;
import com.gem.evidencegraph.temporal.TemporalService;
import com.gem.evidencegraph.temporal.TemporalStatus;
import com.gem.evidencegraph.temporal.dto.BidTemporalEvaluationResultDto;
import com.gem.evidencegraph.temporal.dto.TemporalEvaluationResponseDto;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private EvidenceRelationshipRepository evidenceRelationshipRepository;

    @Mock
    private EntityResolutionResultRepository entityResolutionResultRepository;

    @Mock
    private BidRiskAssessmentRepository bidRiskAssessmentRepository;

    @Mock
    private ComplianceService complianceService;

    @Mock
    private TemporalService temporalService;

    @Mock
    private EntityResolutionService entityResolutionService;

    private RiskService riskService;
    private Bid testBid;
    private Tender testTender;
    private Bidder testBidder;

    @BeforeEach
    void setUp() {
        RiskAssessmentEngine engine = new RiskAssessmentEngine(
                new EligibilityRiskEvaluator(),
                new DocumentIntegrityRiskEvaluator(),
                new ConsistencyRiskEvaluator(),
                new TemporalRiskEvaluator(),
                new VerificationRiskEvaluator(),
                new HardFailEvaluator(),
                new DecisionEngine()
        );

        riskService = new RiskServiceImpl(
                bidRepository,
                documentRepository,
                claimRepository,
                evidenceRepository,
                evidenceRelationshipRepository,
                entityResolutionResultRepository,
                bidRiskAssessmentRepository,
                complianceService,
                temporalService,
                entityResolutionService,
                engine
        );

        UUID tenderId = UUID.randomUUID();
        testTender = Tender.builder()
                .id(tenderId)
                .tenderReference("GEM-TND-2026-001")
                .status(TenderStatus.OPEN)
                .build();

        UUID bidderId = UUID.randomUUID();
        testBidder = Bidder.builder()
                .id(bidderId)
                .legalName("Alpha Infotech")
                .pan("ABCDE1234F")
                .gstin("29ABCDE1234F1Z5")
                .build();

        UUID bidId = UUID.randomUUID();
        testBid = Bid.builder()
                .id(bidId)
                .bidReference("GEM-BID-2026-001")
                .tender(testTender)
                .bidder(testBidder)
                .status(BidStatus.SUBMITTED)
                .submissionDate(LocalDateTime.of(2026, 9, 20, 10, 0))
                .build();
    }

    @Test
    @DisplayName("Evaluate bid risk end-to-end and verify deterministic explainability")
    void testEvaluateBidRiskExplainability() {
        UUID bidId = testBid.getId();
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));

        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .originalFileName("gst_cert.pdf")
                .documentType(DocumentType.GST_CERTIFICATE)
                .integrityStatus(IntegrityStatus.VALID)
                .build();
        when(documentRepository.findByBidId(bidId)).thenReturn(List.of(doc));
        when(claimRepository.findByDocumentBidId(bidId)).thenReturn(Collections.emptyList());

        Evidence ev = Evidence.builder()
                .id(UUID.randomUUID())
                .sourceType(SourceType.GST)
                .attribute("GSTIN")
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        when(evidenceRepository.findByBidId(bidId)).thenReturn(List.of(ev));

        ComplianceEvaluationResultDto compReq = ComplianceEvaluationResultDto.builder()
                .requirementId(UUID.randomUUID())
                .requirementCode("GST_REQ")
                .requirementName("GST Registration")
                .mandatory(true)
                .status(ComplianceStatus.COMPLIANT)
                .build();
        when(complianceService.getLatestBidCompliance(bidId)).thenReturn(BidComplianceResultDto.builder()
                .requirementResults(List.of(compReq))
                .build());

        TemporalEvaluationResponseDto tempDto = TemporalEvaluationResponseDto.builder()
                .evidenceId(ev.getId())
                .status(TemporalStatus.VALID_AT_BID_DATE)
                .reason("Evidence validity covers bid date")
                .build();
        when(temporalService.getBidTemporal(bidId)).thenReturn(BidTemporalEvaluationResultDto.builder()
                .results(List.of(tempDto))
                .build());

        when(bidRiskAssessmentRepository.findByBidId(bidId)).thenReturn(Optional.empty());
        when(bidRiskAssessmentRepository.save(any(BidRiskAssessment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BidRiskAssessmentResponseDto response = riskService.evaluateBidRisk(bidId);

        assertNotNull(response);
        assertEquals(bidId, response.getBidId());
        assertEquals(0, response.getOverallRiskScore());
        assertEquals(RiskLevel.LOW, response.getOverallRiskLevel());
        assertEquals(DecisionRecommendation.RECOMMEND, response.getDecisionRecommendation());
        assertFalse(response.isHardFail());
        assertNotNull(response.getSummary());
        assertNotNull(response.getReason());
        assertEquals(5, response.getDimensions().size());

        for (RiskDimensionResultDto dimDto : response.getDimensions()) {
            assertNotNull(dimDto.getDimension());
            assertNotNull(dimDto.getRiskLevel());
            assertTrue(dimDto.getRiskScore() >= 0 && dimDto.getRiskScore() <= 100);
            assertNotNull(dimDto.getSummary());

            for (RiskFindingDto finding : dimDto.getFindings()) {
                assertNotNull(finding.getCode(), "Every finding must have a code");
                assertNotNull(finding.getTitle(), "Every finding must have a title");
                assertNotNull(finding.getDescription(), "Every finding must have a reason/description");
                assertNotNull(finding.getSourceType(), "Every finding must have a source type");
                assertNotNull(finding.getSourceId(), "Every finding must have a source ID");
                assertNotNull(finding.getRule(), "Every finding must have a rule applied");
                assertNotNull(finding.getSeverity(), "Every finding must have a severity");
                assertTrue(finding.getWeight() >= 0, "Weight must be non-negative");
            }
        }
    }

    @Test
    @DisplayName("Idempotency: Repeated evaluation updates existing record without duplicate assessments")
    void testIdempotentEvaluation() {
        UUID bidId = testBid.getId();
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(testBid));

        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .originalFileName("doc.pdf")
                .documentType(DocumentType.OTHER)
                .integrityStatus(IntegrityStatus.VALID)
                .build();
        when(documentRepository.findByBidId(bidId)).thenReturn(List.of(doc));

        BidRiskAssessment existingRecord = BidRiskAssessment.builder()
                .id(UUID.randomUUID())
                .bid(testBid)
                .overallRiskLevel(RiskLevel.LOW)
                .overallRiskScore(0)
                .decisionRecommendation(DecisionRecommendation.RECOMMEND)
                .hardFail(false)
                .dimensionAssessments(new ArrayList<>())
                .evaluatedAt(LocalDateTime.now().minusHours(1))
                .build();

        when(bidRiskAssessmentRepository.findByBidId(bidId)).thenReturn(Optional.of(existingRecord));
        when(bidRiskAssessmentRepository.save(any(BidRiskAssessment.class))).thenAnswer(i -> i.getArgument(0));

        BidRiskAssessmentResponseDto firstRun = riskService.evaluateBidRisk(bidId);

        BidRiskAssessmentResponseDto secondRun = riskService.evaluateBidRisk(bidId);

        assertEquals(firstRun.getOverallRiskScore(), secondRun.getOverallRiskScore());
        assertEquals(firstRun.getOverallRiskLevel(), secondRun.getOverallRiskLevel());
        assertEquals(firstRun.getDecisionRecommendation(), secondRun.getDecisionRecommendation());

        verify(bidRiskAssessmentRepository, times(2)).save(existingRecord);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when bid ID does not exist")
    void testBidNotFoundThrowsException() {
        UUID unknownBidId = UUID.randomUUID();
        when(bidRepository.findById(unknownBidId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> riskService.evaluateBidRisk(unknownBidId));
    }

}
