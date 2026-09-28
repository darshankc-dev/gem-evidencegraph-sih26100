package com.gem.evidencegraph.verification;

import com.gem.evidencegraph.dto.BulkVerificationResponseDto;
import com.gem.evidencegraph.dto.VerificationRequestDto;
import com.gem.evidencegraph.dto.VerificationResponseDto;
import com.gem.evidencegraph.dto.VerificationResultDto;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.EvidenceRelationship;
import com.gem.evidencegraph.entity.ExtractionMethod;
import com.gem.evidencegraph.entity.RelationshipType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.VerificationStatus;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.ClaimRepository;
import com.gem.evidencegraph.repository.EvidenceRelationshipRepository;
import com.gem.evidencegraph.repository.EvidenceRepository;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import com.gem.evidencegraph.verification.adapter.MockDebarmentVerificationAdapter;
import com.gem.evidencegraph.verification.adapter.MockGstVerificationAdapter;
import com.gem.evidencegraph.verification.adapter.MockPanVerificationAdapter;
import com.gem.evidencegraph.verification.adapter.MockUdyamVerificationAdapter;
import com.gem.evidencegraph.verification.gateway.VerificationGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationGatewayTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private EvidenceRelationshipRepository evidenceRelationshipRepository;

    @Mock
    private BidRepository bidRepository;

    private MockGstVerificationAdapter gstAdapter;
    private MockPanVerificationAdapter panAdapter;
    private MockUdyamVerificationAdapter udyamAdapter;
    private MockDebarmentVerificationAdapter debarmentAdapter;

    private VerificationGatewayService gatewayService;

    @BeforeEach
    void setUp() {
        gstAdapter = new MockGstVerificationAdapter();
        panAdapter = new MockPanVerificationAdapter();
        udyamAdapter = new MockUdyamVerificationAdapter();
        debarmentAdapter = new MockDebarmentVerificationAdapter();

        gatewayService = new VerificationGatewayService(
                claimRepository,
                evidenceRepository,
                evidenceRelationshipRepository,
                bidRepository,
                List.of(gstAdapter, panAdapter, udyamAdapter, debarmentAdapter)
        );
    }

    private Claim createClaim(String fieldName, String value) {
        Bid bid = Bid.builder().id(UUID.randomUUID()).build();
        Document doc = Document.builder().id(UUID.randomUUID()).bid(bid).build();
        return Claim.builder()
                .id(UUID.randomUUID())
                .fieldName(fieldName)
                .fieldValue(value)
                .normalizedValue(value)
                .confidence(0.95)
                .extractionMethod(ExtractionMethod.DETERMINISTIC)
                .document(doc)
                .build();
    }

    @Test
    @DisplayName("1. GST claim verified creates Evidence and VERIFIED_BY relationship")
    void shouldVerifyGstClaimSuccessfully() {
        Claim claim = createClaim("GSTIN", "29ABCDE1234F1Z5");
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(evidenceRelationshipRepository.save(any(EvidenceRelationship.class))).thenAnswer(i -> {
            EvidenceRelationship r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        VerificationRequestDto request = VerificationRequestDto.builder()
                .claimId(claim.getId())
                .requestedSourceType(SourceType.GST)
                .build();

        VerificationResponseDto response = gatewayService.verifyClaim(request);

        assertNotNull(response);
        assertEquals(VerificationStatus.VERIFIED, response.getVerificationStatus());
        assertEquals("29ABCDE1234F1Z5", response.getExpectedValue());
        assertEquals("29ABCDE1234F1Z5", response.getObservedValue());
        assertEquals(1.0, response.getConfidence());
        assertNotNull(response.getEvidenceId());
        assertNotNull(response.getRelationshipId());

        verify(evidenceRelationshipRepository).save(any(EvidenceRelationship.class));
    }

    @Test
    @DisplayName("2. GST claim mismatch creates Evidence and CONTRADICTS relationship")
    void shouldHandleGstClaimMismatch() {
        Claim claim = createClaim("GSTIN", "29ABCDE1234F1Z5-MISMATCH");
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(evidenceRelationshipRepository.save(any(EvidenceRelationship.class))).thenAnswer(i -> {
            EvidenceRelationship r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        VerificationRequestDto request = VerificationRequestDto.builder()
                .claimId(claim.getId())
                .requestedSourceType(SourceType.GST)
                .build();

        VerificationResponseDto response = gatewayService.verifyClaim(request);

        assertEquals(VerificationStatus.MISMATCH, response.getVerificationStatus());
        assertNotNull(response.getRelationshipId());

        ArgumentCaptor<EvidenceRelationship> captor = ArgumentCaptor.forClass(EvidenceRelationship.class);
        verify(evidenceRelationshipRepository).save(captor.capture());
        assertEquals(RelationshipType.CONTRADICTS, captor.getValue().getRelationshipType());
    }

    @Test
    @DisplayName("3. GST source unavailable produces SOURCE_UNAVAILABLE without VERIFIED_BY relationship")
    void shouldHandleGstSourceUnavailable() {
        Claim claim = createClaim("GSTIN", "29ABCDE1234F1Z5");
        gstAdapter.setSimulateUnavailable(true);

        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        VerificationRequestDto request = VerificationRequestDto.builder()
                .claimId(claim.getId())
                .requestedSourceType(SourceType.GST)
                .build();

        VerificationResponseDto response = gatewayService.verifyClaim(request);

        assertEquals(VerificationStatus.SOURCE_UNAVAILABLE, response.getVerificationStatus());
        assertNull(response.getRelationshipId());
        verify(evidenceRelationshipRepository, never()).save(any(EvidenceRelationship.class));
    }

    @Test
    @DisplayName("4. GST claim unverified produces UNVERIFIED status without relationship")
    void shouldHandleGstClaimUnverified() {
        Claim claim = createClaim("GSTIN", "29ABCDE1234F1Z5-UNVERIFIED");
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        VerificationRequestDto request = VerificationRequestDto.builder()
                .claimId(claim.getId())
                .requestedSourceType(SourceType.GST)
                .build();

        VerificationResponseDto response = gatewayService.verifyClaim(request);

        assertEquals(VerificationStatus.UNVERIFIED, response.getVerificationStatus());
        assertNull(response.getRelationshipId());
        verify(evidenceRelationshipRepository, never()).save(any(EvidenceRelationship.class));
    }

    @Test
    @DisplayName("5. Unsupported source type or unsupported claim field handling")
    void shouldHandleUnsupportedSource() {
        Claim claim = createClaim("CERTIFICATE_NUMBER", "CERT-1234");
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        VerificationRequestDto request = VerificationRequestDto.builder()
                .claimId(claim.getId())
                .requestedSourceType(SourceType.PAN)
                .build();

        VerificationResponseDto response = gatewayService.verifyClaim(request);
        assertEquals(VerificationStatus.NOT_APPLICABLE, response.getVerificationStatus());

        VerificationRequestDto invalidSourceRequest = VerificationRequestDto.builder()
                .claimId(claim.getId())
                .requestedSourceType(SourceType.MCA)
                .build();

        assertThrows(IllegalArgumentException.class, () -> gatewayService.verifyClaim(invalidSourceRequest));
    }

    @Test
    @DisplayName("6. Missing claim throws ResourceNotFoundException")
    void shouldThrowWhenClaimNotFound() {
        UUID missingClaimId = UUID.randomUUID();
        when(claimRepository.findById(missingClaimId)).thenReturn(Optional.empty());

        VerificationRequestDto request = VerificationRequestDto.builder()
                .claimId(missingClaimId)
                .requestedSourceType(SourceType.GST)
                .build();

        assertThrows(ResourceNotFoundException.class, () -> gatewayService.verifyClaim(request));
    }

    @Test
    @DisplayName("7. Adapter exception is caught and mapped to SOURCE_UNAVAILABLE without crashing")
    void shouldCatchAdapterExceptionSafely() {
        Claim claim = createClaim("GSTIN", "29ABCDE1234F1Z5");
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        VerificationAdapter throwingAdapter = new VerificationAdapter() {
            @Override
            public SourceType getSourceType() { return SourceType.GST; }
            @Override
            public String getAdapterVersion() { return "throw-v1"; }
            @Override
            public boolean supports(Claim claim) { return true; }
            @Override
            public VerificationResultDto verify(Claim claim) {
                throw new RuntimeException("Simulated socket connection reset");
            }
        };

        VerificationGatewayService failingGateway = new VerificationGatewayService(
                claimRepository, evidenceRepository, evidenceRelationshipRepository, bidRepository,
                List.of(throwingAdapter)
        );

        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        VerificationRequestDto request = VerificationRequestDto.builder()
                .claimId(claim.getId())
                .requestedSourceType(SourceType.GST)
                .build();

        VerificationResponseDto response = failingGateway.verifyClaim(request);

        assertEquals(VerificationStatus.SOURCE_UNAVAILABLE, response.getVerificationStatus());
        assertTrue(response.getReason().contains("Simulated socket connection reset"));
    }

    @Test
    @DisplayName("8. PAN claim verified")
    void shouldVerifyPanSuccessfully() {
        Claim claim = createClaim("PAN", "ABCDE1234F");
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(evidenceRelationshipRepository.save(any(EvidenceRelationship.class))).thenAnswer(i -> {
            EvidenceRelationship r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        VerificationRequestDto request = VerificationRequestDto.builder()
                .claimId(claim.getId())
                .requestedSourceType(SourceType.PAN)
                .build();

        VerificationResponseDto response = gatewayService.verifyClaim(request);

        assertEquals(VerificationStatus.VERIFIED, response.getVerificationStatus());
        assertEquals("ABCDE1234F", response.getObservedValue());
    }

    @Test
    @DisplayName("9. PAN claim mismatch")
    void shouldHandlePanMismatch() {
        Claim claim = createClaim("PAN", "ABCDE1234F-MISMATCH");
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(evidenceRelationshipRepository.save(any(EvidenceRelationship.class))).thenAnswer(i -> {
            EvidenceRelationship r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        VerificationRequestDto request = VerificationRequestDto.builder()
                .claimId(claim.getId())
                .requestedSourceType(SourceType.PAN)
                .build();

        VerificationResponseDto response = gatewayService.verifyClaim(request);

        assertEquals(VerificationStatus.MISMATCH, response.getVerificationStatus());
    }

    @Test
    @DisplayName("10. Udyam claim verified")
    void shouldVerifyUdyamSuccessfully() {
        Claim claim = createClaim("UDYAM_NUMBER", "UDYAM-KR-03-0012345");
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(evidenceRelationshipRepository.save(any(EvidenceRelationship.class))).thenAnswer(i -> {
            EvidenceRelationship r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        VerificationRequestDto request = VerificationRequestDto.builder()
                .claimId(claim.getId())
                .requestedSourceType(SourceType.UDYAM)
                .build();

        VerificationResponseDto response = gatewayService.verifyClaim(request);

        assertEquals(VerificationStatus.VERIFIED, response.getVerificationStatus());
        assertEquals("UDYAM-KR-03-0012345", response.getObservedValue());
    }

    @Test
    @DisplayName("11. Udyam claim mismatch")
    void shouldHandleUdyamMismatch() {
        Claim claim = createClaim("UDYAM_NUMBER", "UDYAM-KR-03-0012345-MISMATCH");
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(evidenceRelationshipRepository.save(any(EvidenceRelationship.class))).thenAnswer(i -> {
            EvidenceRelationship r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        VerificationRequestDto request = VerificationRequestDto.builder()
                .claimId(claim.getId())
                .requestedSourceType(SourceType.UDYAM)
                .build();

        VerificationResponseDto response = gatewayService.verifyClaim(request);

        assertEquals(VerificationStatus.MISMATCH, response.getVerificationStatus());
    }

    @Test
    @DisplayName("12. Debarment source response (verified not-debarred vs debarred mismatch)")
    void shouldHandleDebarmentSourceResponses() {
        Claim cleanClaim = createClaim("PAN", "ABCDE1234F");
        Claim debarredClaim = createClaim("PAN", "ABCDE1234F-DEBARRED");

        when(claimRepository.findById(cleanClaim.getId())).thenReturn(Optional.of(cleanClaim));
        when(claimRepository.findById(debarredClaim.getId())).thenReturn(Optional.of(debarredClaim));
        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        VerificationResponseDto cleanResponse = gatewayService.verifyClaim(
                VerificationRequestDto.builder().claimId(cleanClaim.getId()).requestedSourceType(SourceType.DEBARMENT_LIST).build()
        );
        assertEquals(VerificationStatus.VERIFIED, cleanResponse.getVerificationStatus());
        assertEquals("NOT_DEBARRED", cleanResponse.getObservedValue());

        VerificationResponseDto debarredResponse = gatewayService.verifyClaim(
                VerificationRequestDto.builder().claimId(debarredClaim.getId()).requestedSourceType(SourceType.DEBARMENT_LIST).build()
        );
        assertEquals(VerificationStatus.MISMATCH, debarredResponse.getVerificationStatus());
        assertEquals("DEBARRED", debarredResponse.getObservedValue());
    }

    @Test
    @DisplayName("13, 14, 15, 16. Evidence creation, Claim association, provenance metadata and status persistence")
    void shouldCreateEvidenceWithCompleteProvenance() {
        Claim claim = createClaim("GSTIN", "29ABCDE1234F1Z5");
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        ArgumentCaptor<Evidence> evidenceCaptor = ArgumentCaptor.forClass(Evidence.class);
        when(evidenceRepository.save(evidenceCaptor.capture())).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        gatewayService.verifyClaim(VerificationRequestDto.builder()
                .claimId(claim.getId())
                .requestedSourceType(SourceType.GST)
                .build());

        Evidence saved = evidenceCaptor.getValue();
        assertNotNull(saved);

        assertNotNull(saved);

        assertEquals(claim, saved.getClaim());
        assertEquals(claim.getDocument().getBid(), saved.getBid());

        assertEquals("GSTN-PORTAL-MOCK", saved.getSourceSystem());
        assertEquals("mock-gst-v1.0", saved.getAdapterVersion());
        assertNotNull(saved.getRawResponseSnapshot());
        assertNotNull(saved.getProvenanceHash());
        assertEquals(64, saved.getProvenanceHash().length());

        assertEquals(VerificationStatus.VERIFIED, saved.getVerificationStatus());
    }

    @Test
    @DisplayName("17. VERIFIED_BY relationship created for verified evidence")
    void shouldCreateVerifiedByRelationship() {
        Claim claim = createClaim("PAN", "ABCDE1234F");
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        ArgumentCaptor<EvidenceRelationship> relCaptor = ArgumentCaptor.forClass(EvidenceRelationship.class);
        when(evidenceRelationshipRepository.save(relCaptor.capture())).thenAnswer(i -> {
            EvidenceRelationship r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        gatewayService.verifyClaim(VerificationRequestDto.builder().claimId(claim.getId()).requestedSourceType(SourceType.PAN).build());

        EvidenceRelationship rel = relCaptor.getValue();
        assertEquals(claim, rel.getSourceClaim());
        assertEquals(RelationshipType.VERIFIED_BY, rel.getRelationshipType());
    }

    @Test
    @DisplayName("18. CONTRADICTS relationship created for mismatching evidence")
    void shouldCreateContradictsRelationship() {
        Claim claim = createClaim("PAN", "ABCDE1234F-MISMATCH");
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        ArgumentCaptor<EvidenceRelationship> relCaptor = ArgumentCaptor.forClass(EvidenceRelationship.class);
        when(evidenceRelationshipRepository.save(relCaptor.capture())).thenAnswer(i -> {
            EvidenceRelationship r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        gatewayService.verifyClaim(VerificationRequestDto.builder().claimId(claim.getId()).requestedSourceType(SourceType.PAN).build());

        EvidenceRelationship rel = relCaptor.getValue();
        assertEquals(claim, rel.getSourceClaim());
        assertEquals(RelationshipType.CONTRADICTS, rel.getRelationshipType());
    }

    @Test
    @DisplayName("19 & 20. Safety Rule: SOURCE_UNAVAILABLE must never create VERIFIED_BY relationship and never become VERIFIED")
    void shouldEnforceSafetyRuleForUnavailableSource() {
        Claim claim = createClaim("GSTIN", "29ABCDE1234F1Z5-UNAVAILABLE");
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));
        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        VerificationResponseDto response = gatewayService.verifyClaim(
                VerificationRequestDto.builder().claimId(claim.getId()).requestedSourceType(SourceType.GST).build()
        );

        assertNotEquals(VerificationStatus.VERIFIED, response.getVerificationStatus());
        assertEquals(VerificationStatus.SOURCE_UNAVAILABLE, response.getVerificationStatus());
        assertNull(response.getRelationshipId());
        verify(evidenceRelationshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("21. Idempotency: Repeating identical verification returns existing evidence without duplicate creation")
    void shouldBeIdempotentOnRepeatedVerification() {
        Claim claim = createClaim("GSTIN", "29ABCDE1234F1Z5");
        when(claimRepository.findById(claim.getId())).thenReturn(Optional.of(claim));

        Evidence existingEvidence = Evidence.builder()
                .id(UUID.randomUUID())
                .claim(claim)
                .sourceType(SourceType.GST)
                .sourceReference("GSTN-REG-29ABCDE1234F1Z5")
                .value("29ABCDE1234F1Z5")
                .verificationStatus(VerificationStatus.VERIFIED)
                .confidence(1.0)
                .build();

        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(
                claim.getId(), SourceType.GST, "GSTN-REG-29ABCDE1234F1Z5", "29ABCDE1234F1Z5", VerificationStatus.VERIFIED
        )).thenReturn(Optional.of(existingEvidence));

        VerificationRequestDto request = VerificationRequestDto.builder()
                .claimId(claim.getId())
                .requestedSourceType(SourceType.GST)
                .build();

        VerificationResponseDto response = gatewayService.verifyClaim(request);

        assertEquals(existingEvidence.getId(), response.getEvidenceId());
        assertTrue(response.getReason().contains("Idempotent"));

        verify(evidenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("22. Bulk verification processes all claims for a Bid")
    void shouldExecuteBulkVerificationForBid() {
        UUID bidId = UUID.randomUUID();
        Claim gstinClaim = createClaim("GSTIN", "29ABCDE1234F1Z5");
        Claim panClaim = createClaim("PAN", "ABCDE1234F");

        when(claimRepository.findByDocumentBidId(bidId)).thenReturn(List.of(gstinClaim, panClaim));
        when(claimRepository.findById(gstinClaim.getId())).thenReturn(Optional.of(gstinClaim));
        when(claimRepository.findById(panClaim.getId())).thenReturn(Optional.of(panClaim));
        when(evidenceRepository.findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(i -> {
            Evidence e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        BulkVerificationResponseDto bulkResponse = gatewayService.verifyBidClaims(bidId, null);

        assertNotNull(bulkResponse);
        assertEquals(bidId, bulkResponse.getBidId());
        assertEquals(2, bulkResponse.getTotalClaimsScanned());
        assertEquals(2, bulkResponse.getVerifiedCount());
        assertEquals(2, bulkResponse.getResults().size());
    }

}
