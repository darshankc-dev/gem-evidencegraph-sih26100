package com.gem.evidencegraph.demo;

import com.gem.evidencegraph.compliance.ComplianceService;
import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.compliance.dto.CreateRequirementRequestDto;
import com.gem.evidencegraph.demo.dto.DemoBidSummaryDto;
import com.gem.evidencegraph.demo.dto.DemoDatasetResponseDto;
import com.gem.evidencegraph.demo.dto.DemoResetResponseDto;
import com.gem.evidencegraph.dto.DocumentResponseDto;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.BidStatus;
import com.gem.evidencegraph.entity.Bidder;
import com.gem.evidencegraph.entity.ComplianceRequirement;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.EntityResolutionResult;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.IntegrityStatus;
import com.gem.evidencegraph.entity.RequirementType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.Tender;
import com.gem.evidencegraph.entity.TenderStatus;
import com.gem.evidencegraph.entityresolution.EntityMatchStatus;
import com.gem.evidencegraph.entityresolution.EntityResolutionService;
import com.gem.evidencegraph.extraction.DocumentExtractionPipelineService;
import com.gem.evidencegraph.repository.BidComplianceEvaluationRepository;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.BidRiskAssessmentRepository;
import com.gem.evidencegraph.repository.BidderRepository;
import com.gem.evidencegraph.repository.ClaimRepository;
import com.gem.evidencegraph.repository.ComplianceRequirementRepository;
import com.gem.evidencegraph.repository.DocumentRepository;
import com.gem.evidencegraph.repository.EntityResolutionResultRepository;
import com.gem.evidencegraph.repository.EvidenceRelationshipRepository;
import com.gem.evidencegraph.repository.EvidenceRepository;
import com.gem.evidencegraph.repository.RiskAssessmentRepository;
import com.gem.evidencegraph.repository.RiskFindingRepository;
import com.gem.evidencegraph.repository.TemporalEvaluationRepository;
import com.gem.evidencegraph.repository.TenderRepository;
import com.gem.evidencegraph.risk.DecisionRecommendation;
import com.gem.evidencegraph.risk.RiskLevel;
import com.gem.evidencegraph.risk.RiskService;
import com.gem.evidencegraph.risk.dto.BidRiskAssessmentResponseDto;
import com.gem.evidencegraph.service.DocumentIngestionService;
import com.gem.evidencegraph.service.DocumentStorageService;
import com.gem.evidencegraph.temporal.TemporalService;
import com.gem.evidencegraph.temporal.TemporalStatus;
import com.gem.evidencegraph.temporal.dto.BidTemporalEvaluationResultDto;
import com.gem.evidencegraph.temporal.dto.TemporalEvaluationResponseDto;
import com.gem.evidencegraph.verification.adapter.MockDebarmentVerificationAdapter;
import com.gem.evidencegraph.verification.adapter.MockGstVerificationAdapter;
import com.gem.evidencegraph.verification.adapter.MockPanVerificationAdapter;
import com.gem.evidencegraph.verification.adapter.MockUdyamVerificationAdapter;
import com.gem.evidencegraph.verification.gateway.VerificationGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemoDatasetServiceTest {

    @Mock
    private TenderRepository tenderRepository;
    @Mock
    private BidderRepository bidderRepository;
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
    private ComplianceRequirementRepository complianceRequirementRepository;
    @Mock
    private BidComplianceEvaluationRepository bidComplianceEvaluationRepository;
    @Mock
    private TemporalEvaluationRepository temporalEvaluationRepository;
    @Mock
    private BidRiskAssessmentRepository bidRiskAssessmentRepository;
    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;
    @Mock
    private RiskFindingRepository riskFindingRepository;
    @Mock
    private EntityResolutionResultRepository entityResolutionResultRepository;

    @Mock
    private DocumentIngestionService documentIngestionService;
    @Mock
    private DocumentStorageService documentStorageService;
    @Mock
    private DocumentExtractionPipelineService documentExtractionPipelineService;
    @Mock
    private VerificationGateway verificationGateway;
    @Mock
    private MockGstVerificationAdapter mockGstAdapter;
    @Mock
    private MockPanVerificationAdapter mockPanAdapter;
    @Mock
    private MockUdyamVerificationAdapter mockUdyamAdapter;
    @Mock
    private MockDebarmentVerificationAdapter mockDebarmentAdapter;
    @Mock
    private EntityResolutionService entityResolutionService;
    @Mock
    private ComplianceService complianceService;
    @Mock
    private TemporalService temporalService;
    @Mock
    private RiskService riskService;

    private DemoDatasetServiceImpl demoDatasetService;

    @BeforeEach
    void setUp() {
        demoDatasetService = new DemoDatasetServiceImpl(
                tenderRepository,
                bidderRepository,
                bidRepository,
                documentRepository,
                claimRepository,
                evidenceRepository,
                evidenceRelationshipRepository,
                complianceRequirementRepository,
                bidComplianceEvaluationRepository,
                temporalEvaluationRepository,
                bidRiskAssessmentRepository,
                riskAssessmentRepository,
                riskFindingRepository,
                entityResolutionResultRepository,
                documentIngestionService,
                documentStorageService,
                documentExtractionPipelineService,
                verificationGateway,
                mockGstAdapter,
                mockPanAdapter,
                mockUdyamAdapter,
                mockDebarmentAdapter,
                entityResolutionService,
                complianceService,
                temporalService,
                riskService
        );
    }

    @Test
    @DisplayName("1. loadDemoDataset executes full pipeline for 3 bids when not already loaded")
    void shouldLoadDemoDatasetSuccessfully() {
        when(tenderRepository.findByTenderReference(DemoDatasetServiceImpl.DEMO_TENDER_REF))
                .thenReturn(Optional.empty());

        UUID tenderId = UUID.randomUUID();
        when(tenderRepository.save(any(Tender.class))).thenAnswer(inv -> {
            Tender t = inv.getArgument(0);
            t.setId(tenderId);
            return t;
        });

        when(bidderRepository.save(any(Bidder.class))).thenAnswer(inv -> {
            Bidder b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> {
            Bid b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        UUID docId = UUID.randomUUID();
        DocumentResponseDto docDto = DocumentResponseDto.builder()
                .documentId(docId)
                .fileName("doc.pdf")
                .originalFileName("doc.pdf")
                .documentType(DocumentType.OTHER)
                .integrityStatus(IntegrityStatus.NOT_CHECKED)
                .build();
        when(documentIngestionService.uploadDocument(any(), any())).thenReturn(docDto);

        Document docEntity = Document.builder()
                .id(docId)
                .documentType(DocumentType.OTHER)
                .integrityStatus(IntegrityStatus.NOT_CHECKED)
                .build();
        when(documentRepository.findById(docId)).thenReturn(Optional.of(docEntity));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        when(claimRepository.findByDocumentBidId(any())).thenReturn(Collections.emptyList());
        when(entityResolutionResultRepository.findByBidderIdOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(EntityResolutionResult.builder().matchStatus(EntityMatchStatus.MATCH).build()));

        when(riskService.getBidRisk(any())).thenReturn(BidRiskAssessmentResponseDto.builder()
                .overallRiskLevel(RiskLevel.LOW)
                .overallRiskScore(10)
                .decisionRecommendation(DecisionRecommendation.RECOMMEND)
                .build());

        when(temporalService.getBidTemporal(any())).thenReturn(BidTemporalEvaluationResultDto.builder()
                .results(List.of(TemporalEvaluationResponseDto.builder().status(TemporalStatus.VALID_AT_BID_DATE).build()))
                .build());

        DemoDatasetResponseDto response = demoDatasetService.loadDemoDataset();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("LOADED");
        assertThat(response.getTenderReference()).isEqualTo(DemoDatasetServiceImpl.DEMO_TENDER_REF);
        assertThat(response.getDatasetLabel()).isEqualTo(DemoDatasetService.DATASET_LABEL);
        assertThat(response.getTotalBidders()).isEqualTo(3);
        assertThat(response.getTotalBids()).isEqualTo(3);
        assertThat(response.getBids()).hasSize(3);

        verify(tenderRepository, times(1)).save(any(Tender.class));

        verify(complianceService, times(5)).createRequirement(eq(tenderId), any(CreateRequirementRequestDto.class));

        verify(documentIngestionService, times(15)).uploadDocument(any(), any());

        verify(verificationGateway, times(3)).verifyBidClaims(any(), any());
        verify(entityResolutionService, times(3)).resolveBidderAgainstEvidence(any());
        verify(complianceService, times(3)).evaluateBidCompliance(any());
        verify(temporalService, times(3)).evaluateBidTemporal(any());
        verify(riskService, times(3)).evaluateBidRisk(any());
    }

    @Test
    @DisplayName("2. loadDemoDataset is idempotent: returns ALREADY_LOADED without re-creating records")
    void shouldReturnAlreadyLoadedWhenTenderExists() {
        Tender existingTender = Tender.builder()
                .id(UUID.randomUUID())
                .tenderReference(DemoDatasetServiceImpl.DEMO_TENDER_REF)
                .title("Supply of Network Security Equipment and Services")
                .status(TenderStatus.OPEN)
                .build();

        Bid existingBid = Bid.builder()
                .id(UUID.randomUUID())
                .bidReference(DemoDatasetServiceImpl.DEMO_BID_A_REF)
                .bidder(Bidder.builder().id(UUID.randomUUID()).legalName("Apex Secure Systems Pvt Ltd").build())
                .tender(existingTender)
                .status(BidStatus.SUBMITTED)
                .build();

        when(tenderRepository.findByTenderReference(DemoDatasetServiceImpl.DEMO_TENDER_REF))
                .thenReturn(Optional.of(existingTender));
        when(bidRepository.findByTenderId(existingTender.getId()))
                .thenReturn(List.of(existingBid));

        DemoDatasetResponseDto response = demoDatasetService.loadDemoDataset();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ALREADY_LOADED");
        assertThat(response.getTenderReference()).isEqualTo(DemoDatasetServiceImpl.DEMO_TENDER_REF);
        assertThat(response.getMessage()).contains("already loaded");

        verify(tenderRepository, times(0)).save(any(Tender.class));
    }

    @Test
    @DisplayName("3. resetDemoDataset removes all demo records in referential integrity order")
    void shouldResetDemoDatasetSuccessfully() {
        UUID tenderId = UUID.randomUUID();
        UUID bidId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();

        Tender tender = Tender.builder().id(tenderId).tenderReference(DemoDatasetServiceImpl.DEMO_TENDER_REF).build();
        Bidder bidder = Bidder.builder().id(bidderId).legalName("Apex Secure Systems").build();
        Bid bid = Bid.builder().id(bidId).bidReference(DemoDatasetServiceImpl.DEMO_BID_A_REF).bidder(bidder).tender(tender).build();

        when(tenderRepository.findByTenderReference(DemoDatasetServiceImpl.DEMO_TENDER_REF))
                .thenReturn(Optional.of(tender));
        when(bidRepository.findByTenderId(tenderId)).thenReturn(new ArrayList<>(List.of(bid)));

        UUID docId = UUID.randomUUID();
        Document doc = Document.builder().id(docId).storagePath("storage/test.pdf").build();
        when(documentRepository.findByBidId(bidId)).thenReturn(List.of(doc));

        when(complianceRequirementRepository.findByTenderId(tenderId)).thenReturn(Collections.emptyList());

        DemoResetResponseDto resetResult = demoDatasetService.resetDemoDataset();

        assertThat(resetResult).isNotNull();
        assertThat(resetResult.getStatus()).isEqualTo("RESET_SUCCESSFUL");
        assertThat(resetResult.getDeletedTenders()).isEqualTo(1);
        assertThat(resetResult.getDeletedBids()).isEqualTo(1);
        assertThat(resetResult.getDeletedBidders()).isEqualTo(1);
        assertThat(resetResult.getDeletedDocuments()).isEqualTo(1);

        verify(riskFindingRepository).deleteByBidId(bidId);
        verify(temporalEvaluationRepository).deleteByBidId(bidId);
        verify(bidRiskAssessmentRepository).deleteByBidId(bidId);
        verify(documentRepository).delete(doc);
        verify(bidRepository).delete(bid);
        verify(bidderRepository).delete(bidder);
        verify(tenderRepository).delete(tender);

        verify(mockGstAdapter).clearMockResponses();
        verify(mockDebarmentAdapter).clearMockResponses();
    }

    @Test
    @DisplayName("4. isDemoDatasetLoaded returns true if tender exists, false otherwise")
    void shouldCheckIfDemoDatasetIsLoaded() {
        when(tenderRepository.findByTenderReference(DemoDatasetServiceImpl.DEMO_TENDER_REF))
                .thenReturn(Optional.empty());
        assertThat(demoDatasetService.isDemoDatasetLoaded()).isFalse();

        when(tenderRepository.findByTenderReference(DemoDatasetServiceImpl.DEMO_TENDER_REF))
                .thenReturn(Optional.of(Tender.builder().tenderReference(DemoDatasetServiceImpl.DEMO_TENDER_REF).build()));
        assertThat(demoDatasetService.isDemoDatasetLoaded()).isTrue();
    }

}
