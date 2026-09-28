package com.gem.evidencegraph.compliance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.compliance.dto.CreateRequirementRequestDto;
import com.gem.evidencegraph.compliance.dto.RequirementResponseDto;
import com.gem.evidencegraph.compliance.rule.ComplianceRuleEngine;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.BidComplianceEvaluation;
import com.gem.evidencegraph.entity.Bidder;
import com.gem.evidencegraph.entity.ComplianceRequirement;
import com.gem.evidencegraph.entity.RequirementType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.Tender;
import com.gem.evidencegraph.repository.BidComplianceEvaluationRepository;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.ClaimRepository;
import com.gem.evidencegraph.repository.ComplianceRequirementRepository;
import com.gem.evidencegraph.repository.DocumentRepository;
import com.gem.evidencegraph.repository.EvidenceRepository;
import com.gem.evidencegraph.repository.TenderRepository;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock
    private TenderRepository tenderRepository;
    @Mock
    private BidRepository bidRepository;
    @Mock
    private ComplianceRequirementRepository requirementRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private BidComplianceEvaluationRepository evaluationRepository;
    @Mock
    private ComplianceRuleEngine complianceRuleEngine;

    private ComplianceServiceImpl complianceService;
    private ObjectMapper objectMapper;

    private UUID tenderId;
    private UUID bidId;
    private UUID bidderId;
    private UUID reqId;
    private Tender tender;
    private Bidder bidder;
    private Bid bid;
    private ComplianceRequirement requirement;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        complianceService = new ComplianceServiceImpl(
                tenderRepository,
                bidRepository,
                requirementRepository,
                documentRepository,
                claimRepository,
                evidenceRepository,
                evaluationRepository,
                complianceRuleEngine,
                objectMapper
        );

        tenderId = UUID.randomUUID();
        bidId = UUID.randomUUID();
        bidderId = UUID.randomUUID();
        reqId = UUID.randomUUID();

        tender = Tender.builder()
                .id(tenderId)
                .tenderReference("GEM/2026/B/1001")
                .title("Procurement of IT Hardware")
                .build();

        bidder = Bidder.builder()
                .id(bidderId)
                .legalName("ABC Technologies Pvt Ltd")
                .build();

        bid = Bid.builder()
                .id(bidId)
                .bidReference("BID-1001-A")
                .tender(tender)
                .bidder(bidder)
                .build();

        requirement = ComplianceRequirement.builder()
                .id(reqId)
                .tender(tender)
                .requirementCode("REQ-GST")
                .name("GST Registration Requirement")
                .description("Bidder must possess a valid GSTIN")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(true)
                .expectedSourceType(SourceType.GST)
                .build();
    }

    @Test
    @DisplayName("Create tender requirement succeeds when tender exists")
    void createRequirement_success() {
        CreateRequirementRequestDto request = CreateRequirementRequestDto.builder()
                .requirementCode("REQ-PAN")
                .name("Valid PAN Requirement")
                .description("PAN must be active")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(true)
                .expectedSourceType(SourceType.PAN)
                .build();

        when(tenderRepository.findById(tenderId)).thenReturn(Optional.of(tender));
        when(requirementRepository.findByTenderIdAndRequirementCode(tenderId, "REQ-PAN")).thenReturn(Optional.empty());
        when(requirementRepository.save(any(ComplianceRequirement.class))).thenAnswer(invocation -> {
            ComplianceRequirement r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        RequirementResponseDto response = complianceService.createRequirement(tenderId, request);

        assertThat(response).isNotNull();
        assertThat(response.getRequirementCode()).isEqualTo("REQ-PAN");
        assertThat(response.getName()).isEqualTo("Valid PAN Requirement");
        assertThat(response.getTenderId()).isEqualTo(tenderId);
        assertThat(response.isMandatory()).isTrue();
    }

    @Test
    @DisplayName("Create tender requirement throws ResourceNotFoundException if tender does not exist")
    void createRequirement_tenderNotFound() {
        CreateRequirementRequestDto request = CreateRequirementRequestDto.builder()
                .requirementCode("REQ-PAN")
                .name("Valid PAN Requirement")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(true)
                .build();

        when(tenderRepository.findById(tenderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complianceService.createRequirement(tenderId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tender with ID");
    }

    @Test
    @DisplayName("Get tender requirements returns requirement list for valid tender")
    void getRequirementsByTender_success() {
        when(tenderRepository.existsById(tenderId)).thenReturn(true);
        when(requirementRepository.findByTenderId(tenderId)).thenReturn(List.of(requirement));

        List<RequirementResponseDto> results = complianceService.getRequirementsByTender(tenderId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRequirementCode()).isEqualTo("REQ-GST");
    }

    @Test
    @DisplayName("Get tender requirements throws ResourceNotFoundException if tender not found")
    void getRequirementsByTender_notFound() {
        when(tenderRepository.existsById(tenderId)).thenReturn(false);

        assertThatThrownBy(() -> complianceService.getRequirementsByTender(tenderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tender with ID");
    }

    @Test
    @DisplayName("Get requirement by ID returns DTO when found")
    void getRequirementById_success() {
        when(requirementRepository.findById(reqId)).thenReturn(Optional.of(requirement));

        RequirementResponseDto response = complianceService.getRequirementById(reqId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(reqId);
        assertThat(response.getRequirementCode()).isEqualTo("REQ-GST");
    }

    @Test
    @DisplayName("Get requirement by ID throws ResourceNotFoundException when not found")
    void getRequirementById_notFound() {
        when(requirementRepository.findById(reqId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complianceService.getRequirementById(reqId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Requirement with ID");
    }

    @Test
    @DisplayName("Evaluate bid compliance successfully loads context and returns summary DTO")
    void evaluateBidCompliance_success() {
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(requirementRepository.findByTenderId(tenderId)).thenReturn(List.of(requirement));
        when(documentRepository.findByBidId(bidId)).thenReturn(Collections.emptyList());
        when(claimRepository.findByDocumentBidId(bidId)).thenReturn(Collections.emptyList());
        when(evidenceRepository.findByBidId(bidId)).thenReturn(Collections.emptyList());

        ComplianceEvaluationResultDto evalResult = ComplianceEvaluationResultDto.builder()
                .requirementId(reqId)
                .requirementCode("REQ-GST")
                .requirementName("GST Registration Requirement")
                .mandatory(true)
                .status(ComplianceStatus.COMPLIANT)
                .confidence(1.0)
                .evidenceIds(List.of(UUID.randomUUID()))
                .supportingClaimIds(List.of(UUID.randomUUID()))
                .reason("GST verified")
                .evaluatedAt(LocalDateTime.now())
                .build();

        BidComplianceResultDto ruleEngineResult = BidComplianceResultDto.builder()
                .bidId(bidId)
                .tenderId(tenderId)
                .bidderId(bidderId)
                .overallStatus(OverallComplianceStatus.COMPLIANT)
                .compliancePercentage(100.0)
                .mandatoryRequirementCount(1)
                .compliantCount(1)
                .nonCompliantCount(0)
                .missingCount(0)
                .unverifiedCount(0)
                .contradictoryCount(0)
                .pendingReviewCount(0)
                .notApplicableCount(0)
                .requirementResults(List.of(evalResult))
                .summary("All 1 evaluated requirements are compliant.")
                .evaluatedAt(LocalDateTime.now())
                .build();

        when(complianceRuleEngine.evaluate(any(), any())).thenReturn(ruleEngineResult);

        BidComplianceResultDto result = complianceService.evaluateBidCompliance(bidId);

        assertThat(result).isNotNull();
        assertThat(result.getBidId()).isEqualTo(bidId);
        assertThat(result.getTenderId()).isEqualTo(tenderId);
        assertThat(result.getBidderId()).isEqualTo(bidderId);
        assertThat(result.getOverallStatus()).isEqualTo(OverallComplianceStatus.COMPLIANT);
        assertThat(result.getCompliancePercentage()).isEqualTo(100.0);
        assertThat(result.getMandatoryRequirementCount()).isEqualTo(1);
        assertThat(result.getCompliantCount()).isEqualTo(1);
        assertThat(result.getRequirementResults()).hasSize(1);

        verify(evaluationRepository).save(any(BidComplianceEvaluation.class));
    }

    @Test
    @DisplayName("Evaluate bid compliance throws ResourceNotFoundException when bid not found")
    void evaluateBidCompliance_bidNotFound() {
        when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> complianceService.evaluateBidCompliance(bidId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Bid with ID");
    }
}
