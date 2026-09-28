package com.gem.evidencegraph.temporal;

import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.BidStatus;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.EvidenceType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.TemporalEvaluation;
import com.gem.evidencegraph.entity.Tender;
import com.gem.evidencegraph.entity.TenderStatus;
import com.gem.evidencegraph.entity.VerificationStatus;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.ClaimRepository;
import com.gem.evidencegraph.repository.EvidenceRepository;
import com.gem.evidencegraph.repository.TemporalEvaluationRepository;
import com.gem.evidencegraph.temporal.dto.BidTemporalEvaluationResultDto;
import com.gem.evidencegraph.temporal.dto.TemporalEvaluationResponseDto;
import com.gem.evidencegraph.temporal.rule.DateConsistencyRule;
import com.gem.evidencegraph.temporal.rule.ExpirationRule;
import com.gem.evidencegraph.temporal.rule.FutureValidityRule;
import com.gem.evidencegraph.temporal.rule.IssueAfterBidRule;
import com.gem.evidencegraph.temporal.rule.IssueAfterTenderPublicationRule;
import com.gem.evidencegraph.temporal.rule.MissingTemporalDataRule;
import com.gem.evidencegraph.temporal.rule.NotApplicableRule;
import com.gem.evidencegraph.temporal.rule.ValidityWindowRule;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemporalServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private TemporalEvaluationRepository temporalEvaluationRepository;

    private TemporalService temporalService;

    @BeforeEach
    void setUp() {
        TemporalVerificationEngine engine = new TemporalVerificationEngine(List.of(
                new DateConsistencyRule(),
                new MissingTemporalDataRule(),
                new FutureValidityRule(),
                new ExpirationRule(),
                new IssueAfterBidRule(),
                new IssueAfterTenderPublicationRule(),
                new ValidityWindowRule(),
                new NotApplicableRule()
        ));

        temporalService = new TemporalServiceImpl(
                bidRepository,
                evidenceRepository,
                claimRepository,
                temporalEvaluationRepository,
                engine
        );
    }

    @Test
    @DisplayName("evaluateBidTemporal computes correct statuses and counts across multiple evidence items")
    void testEvaluateBidTemporal_success() {
        UUID bidId = UUID.randomUUID();
        Tender tender = Tender.builder()
                .id(UUID.randomUUID())
                .tenderReference("GEM-TND-2026-001")
                .title("IT Equipment")
                .publicationDate(LocalDateTime.of(2026, 8, 1, 0, 0))
                .submissionDeadline(LocalDateTime.of(2026, 9, 30, 0, 0))
                .status(TenderStatus.OPEN)
                .build();

        Bid bid = Bid.builder()
                .id(bidId)
                .bidReference("GEM-BID-2026-001")
                .tender(tender)
                .submissionDate(LocalDateTime.of(2026, 9, 20, 10, 0))
                .status(BidStatus.SUBMITTED)
                .build();

        Evidence ev1 = Evidence.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .evidenceType(EvidenceType.DOCUMENT)
                .sourceType(SourceType.BID_DOCUMENT)
                .subject("ISO")
                .attribute("CERTIFICATE")
                .validFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .validUntil(LocalDateTime.of(2026, 12, 31, 0, 0))
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        Evidence ev2 = Evidence.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .evidenceType(EvidenceType.DOCUMENT)
                .sourceType(SourceType.BID_DOCUMENT)
                .subject("LICENSE")
                .attribute("CERTIFICATE")
                .validFrom(LocalDateTime.of(2025, 1, 1, 0, 0))
                .validUntil(LocalDateTime.of(2026, 8, 31, 0, 0))
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(claimRepository.findByDocumentBidId(bidId)).thenReturn(Collections.emptyList());
        when(evidenceRepository.findByBidId(bidId)).thenReturn(List.of(ev1, ev2));
        when(temporalEvaluationRepository.findByBidIdAndEvidenceId(any(), any())).thenReturn(Optional.empty());
        when(temporalEvaluationRepository.save(any(TemporalEvaluation.class))).thenAnswer(i -> {
            TemporalEvaluation te = i.getArgument(0);
            te.setId(UUID.randomUUID());
            return te;
        });

        BidTemporalEvaluationResultDto result = temporalService.evaluateBidTemporal(bidId);

        assertNotNull(result);
        assertEquals(bidId, result.getBidId());
        assertEquals(2, result.getTotalEvaluated());
        assertEquals(1, result.getValidCount());
        assertEquals(1, result.getExpiredCount());
        assertEquals(2, result.getResults().size());

        verify(temporalEvaluationRepository, times(2)).save(any(TemporalEvaluation.class));
    }

    @Test
    @DisplayName("evaluateBidTemporal updates existing evaluations idempotently")
    void testEvaluateBidTemporal_idempotency() {
        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder()
                .id(bidId)
                .submissionDate(LocalDateTime.of(2026, 9, 20, 10, 0))
                .status(BidStatus.SUBMITTED)
                .build();

        Evidence ev = Evidence.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .evidenceType(EvidenceType.DOCUMENT)
                .sourceType(SourceType.BID_DOCUMENT)
                .attribute("CERTIFICATE")
                .validFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .validUntil(LocalDateTime.of(2026, 12, 31, 0, 0))
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        TemporalEvaluation existingEval = TemporalEvaluation.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .evidence(ev)
                .status(TemporalStatus.NOT_YET_VALID_AT_BID_DATE)
                .evaluatedAt(LocalDateTime.of(2026, 9, 1, 0, 0))
                .build();

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(claimRepository.findByDocumentBidId(bidId)).thenReturn(Collections.emptyList());
        when(evidenceRepository.findByBidId(bidId)).thenReturn(List.of(ev));
        when(temporalEvaluationRepository.findByBidIdAndEvidenceId(bidId, ev.getId())).thenReturn(Optional.of(existingEval));
        when(temporalEvaluationRepository.save(any(TemporalEvaluation.class))).thenAnswer(i -> i.getArgument(0));

        BidTemporalEvaluationResultDto result = temporalService.evaluateBidTemporal(bidId);

        assertEquals(1, result.getTotalEvaluated());
        assertEquals(TemporalStatus.VALID_AT_BID_DATE, existingEval.getStatus());
        verify(temporalEvaluationRepository, times(1)).save(existingEval);
    }

    @Test
    @DisplayName("evaluateBidTemporal throws ResourceNotFoundException when bid does not exist")
    void testEvaluateBidTemporal_notFound() {
        UUID bidId = UUID.randomUUID();
        when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> temporalService.evaluateBidTemporal(bidId));
        verify(temporalEvaluationRepository, never()).save(any());
    }

    @Test
    @DisplayName("getBidTemporal returns existing evaluations if already evaluated")
    void testGetBidTemporal_existing() {
        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder().id(bidId).submissionDate(LocalDateTime.now()).status(BidStatus.SUBMITTED).build();
        TemporalEvaluation te = TemporalEvaluation.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .status(TemporalStatus.VALID_AT_BID_DATE)
                .evaluatedAt(LocalDateTime.now())
                .build();

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(temporalEvaluationRepository.findByBidIdOrderByEvaluatedAtDesc(bidId)).thenReturn(List.of(te));

        BidTemporalEvaluationResultDto result = temporalService.getBidTemporal(bidId);

        assertNotNull(result);
        assertEquals(1, result.getTotalEvaluated());
        assertEquals(1, result.getValidCount());
        verify(temporalEvaluationRepository, never()).save(any());
    }

    @Test
    @DisplayName("getEvidenceTemporal returns evaluation list for valid evidence")
    void testGetEvidenceTemporal_success() {
        UUID evidenceId = UUID.randomUUID();
        when(evidenceRepository.existsById(evidenceId)).thenReturn(true);

        TemporalEvaluation te = TemporalEvaluation.builder()
                .id(UUID.randomUUID())
                .status(TemporalStatus.VALID_AT_BID_DATE)
                .evaluatedAt(LocalDateTime.now())
                .build();

        when(temporalEvaluationRepository.findByEvidenceIdOrderByEvaluatedAtDesc(evidenceId)).thenReturn(List.of(te));

        List<TemporalEvaluationResponseDto> list = temporalService.getEvidenceTemporal(evidenceId);

        assertEquals(1, list.size());
        assertEquals(TemporalStatus.VALID_AT_BID_DATE, list.get(0).getStatus());
    }

    @Test
    @DisplayName("getEvidenceTemporal throws ResourceNotFoundException for unknown evidence")
    void testGetEvidenceTemporal_notFound() {
        UUID evidenceId = UUID.randomUUID();
        when(evidenceRepository.existsById(evidenceId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> temporalService.getEvidenceTemporal(evidenceId));
    }

    @Test
    @DisplayName("Regression A: Declaration document date must not be inherited by PAN or Debarment evidence")
    void testDeclarationDateIsolation_panAndDebarmentDoNotInheritDeclarationRegistrationDate() {
        UUID bidId = UUID.randomUUID();
        Tender tender = Tender.builder()
                .id(UUID.randomUUID())
                .tenderReference("GEM-TND-2026-DEMO")
                .publicationDate(LocalDateTime.of(2026, 8, 1, 10, 0))
                .submissionDeadline(LocalDateTime.of(2026, 8, 20, 17, 0))
                .status(TenderStatus.OPEN)
                .build();

        Bid bid = Bid.builder()
                .id(bidId)
                .bidReference("GEM-BID-DEMO-001")
                .tender(tender)
                .submissionDate(LocalDateTime.of(2026, 8, 15, 14, 30))
                .status(BidStatus.SUBMITTED)
                .build();

        Document declDoc = Document.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .fileName("Apex_Blacklisting_Declaration.pdf")
                .documentType(DocumentType.BLACKLIST_DECLARATION)
                .build();

        Claim panClaim = Claim.builder()
                .id(UUID.randomUUID())
                .document(declDoc)
                .fieldName("PAN")
                .fieldValue("ABCDE1234F")
                .normalizedValue("ABCDE1234F")
                .build();

        Claim declDateClaim = Claim.builder()
                .id(UUID.randomUUID())
                .document(declDoc)
                .fieldName("REGISTRATION_DATE")
                .fieldValue("10/08/2026")
                .normalizedValue("2026-08-10")
                .build();

        Evidence panEvidence = Evidence.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .claim(panClaim)
                .evidenceType(EvidenceType.REGISTRY_RECORD)
                .sourceType(SourceType.PAN)
                .subject("PAN")
                .attribute("PAN")
                .value("ABCDE1234F")
                .normalizedValue("ABCDE1234F")
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        Evidence debarmentEvidence = Evidence.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .claim(panClaim)
                .evidenceType(EvidenceType.REGISTRY_RECORD)
                .sourceType(SourceType.DEBARMENT_LIST)
                .subject("PAN")
                .attribute("DEBARMENT_STATUS")
                .value("NOT_DEBARRED")
                .normalizedValue("NOT_DEBARRED")
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(claimRepository.findByDocumentBidId(bidId)).thenReturn(List.of(panClaim, declDateClaim));
        when(evidenceRepository.findByBidId(bidId)).thenReturn(List.of(panEvidence, debarmentEvidence));
        when(temporalEvaluationRepository.findByBidIdAndEvidenceId(any(), any())).thenReturn(Optional.empty());
        when(temporalEvaluationRepository.save(any(TemporalEvaluation.class))).thenAnswer(i -> {
            TemporalEvaluation te = i.getArgument(0);
            te.setId(UUID.randomUUID());
            return te;
        });

        BidTemporalEvaluationResultDto result = temporalService.evaluateBidTemporal(bidId);

        assertNotNull(result);
        assertEquals(0, result.getIssuedAfterTenderPublicationCount(),
                "Declaration date must not cause ISSUED_AFTER_TENDER_PUBLICATION on PAN/Debarment evidence");
        for (TemporalEvaluationResponseDto dto : result.getResults()) {
            assertEquals(TemporalStatus.NOT_APPLICABLE, dto.getStatus());
            org.junit.jupiter.api.Assertions.assertNull(dto.getIssueDate(),
                    "Issue date should be null for un-scoped declaration evidence");
        }
    }

    @Test
    @DisplayName("Regression B: Legitimate registration document preserves registration date inheritance")
    void testRealRegistrationDateBehavior_legitimateRegistrationDocumentInheritsIssueDate() {
        UUID bidId = UUID.randomUUID();
        Tender tender = Tender.builder()
                .id(UUID.randomUUID())
                .tenderReference("GEM-TND-2026-DEMO")
                .publicationDate(LocalDateTime.of(2026, 8, 1, 10, 0))
                .submissionDeadline(LocalDateTime.of(2026, 8, 20, 17, 0))
                .status(TenderStatus.OPEN)
                .build();

        Bid bid = Bid.builder()
                .id(bidId)
                .bidReference("GEM-BID-DEMO-001")
                .tender(tender)
                .submissionDate(LocalDateTime.of(2026, 8, 15, 14, 30))
                .status(BidStatus.SUBMITTED)
                .build();

        Document panDoc = Document.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .fileName("Apex_PAN_Document.pdf")
                .documentType(DocumentType.PAN_DOCUMENT)
                .build();

        Claim panClaim = Claim.builder()
                .id(UUID.randomUUID())
                .document(panDoc)
                .fieldName("PAN")
                .fieldValue("ABCDE1234F")
                .normalizedValue("ABCDE1234F")
                .build();

        Claim regDateClaim = Claim.builder()
                .id(UUID.randomUUID())
                .document(panDoc)
                .fieldName("REGISTRATION_DATE")
                .fieldValue("15/01/2022")
                .normalizedValue("2022-01-15")
                .build();

        Evidence panEvidence = Evidence.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .claim(panClaim)
                .evidenceType(EvidenceType.REGISTRY_RECORD)
                .sourceType(SourceType.PAN)
                .subject("PAN")
                .attribute("PAN")
                .value("ABCDE1234F")
                .normalizedValue("ABCDE1234F")
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(claimRepository.findByDocumentBidId(bidId)).thenReturn(List.of(panClaim, regDateClaim));
        when(evidenceRepository.findByBidId(bidId)).thenReturn(List.of(panEvidence));
        when(temporalEvaluationRepository.findByBidIdAndEvidenceId(any(), any())).thenReturn(Optional.empty());
        when(temporalEvaluationRepository.save(any(TemporalEvaluation.class))).thenAnswer(i -> {
            TemporalEvaluation te = i.getArgument(0);
            te.setId(UUID.randomUUID());
            return te;
        });

        BidTemporalEvaluationResultDto result = temporalService.evaluateBidTemporal(bidId);

        assertNotNull(result);
        assertEquals(1, result.getValidCount());
        assertEquals(1, result.getResults().size());
        TemporalEvaluationResponseDto dto = result.getResults().get(0);
        assertEquals(TemporalStatus.VALID_AT_BID_DATE, dto.getStatus());
        assertEquals(LocalDateTime.of(2022, 1, 15, 0, 0), dto.getIssueDate(),
                "PAN evidence should inherit legitimate registration date from PAN_DOCUMENT");
    }

    @Test
    @DisplayName("Regression C: TemporalEvaluationResponseDto exposes issueDate accurately")
    void testDtoTransparency_exposesIssueDateInResponseDto() {
        UUID evidenceId = UUID.randomUUID();
        when(evidenceRepository.existsById(evidenceId)).thenReturn(true);

        LocalDateTime issueDate = LocalDateTime.of(2024, 4, 10, 0, 0);
        TemporalEvaluation te = TemporalEvaluation.builder()
                .id(UUID.randomUUID())
                .status(TemporalStatus.VALID_AT_BID_DATE)
                .issueDate(issueDate)
                .evaluatedAt(LocalDateTime.now())
                .build();

        when(temporalEvaluationRepository.findByEvidenceIdOrderByEvaluatedAtDesc(evidenceId)).thenReturn(List.of(te));

        List<TemporalEvaluationResponseDto> list = temporalService.getEvidenceTemporal(evidenceId);

        assertEquals(1, list.size());
        assertEquals(issueDate, list.get(0).getIssueDate(), "DTO must expose issueDate for transparency");
    }

}
