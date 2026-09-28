package com.gem.evidencegraph.entityresolution;

import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Bidder;
import com.gem.evidencegraph.entity.EntityResolutionResult;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.EvidenceRelationship;
import com.gem.evidencegraph.entity.EvidenceType;
import com.gem.evidencegraph.entity.RelationshipType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.VerificationStatus;
import com.gem.evidencegraph.entityresolution.dto.EntityProfileDto;
import com.gem.evidencegraph.entityresolution.dto.EntityResolutionResultDto;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.BidderRepository;
import com.gem.evidencegraph.repository.ClaimRepository;
import com.gem.evidencegraph.repository.EntityResolutionResultRepository;
import com.gem.evidencegraph.repository.EvidenceRelationshipRepository;
import com.gem.evidencegraph.repository.EvidenceRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntityResolutionServiceTest {

    private EntityNormalizationService normalizationService;

    @Mock
    private BidderRepository bidderRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private EvidenceRelationshipRepository evidenceRelationshipRepository;

    @Mock
    private EntityResolutionResultRepository entityResolutionResultRepository;

    private EntityResolutionServiceImpl entityResolutionService;

    @BeforeEach
    void setUp() {
        normalizationService = new EntityNormalizationService();
        entityResolutionService = new EntityResolutionServiceImpl(
                normalizationService,
                bidderRepository,
                bidRepository,
                claimRepository,
                evidenceRepository,
                evidenceRelationshipRepository,
                entityResolutionResultRepository
        );
    }

    @Test
    @DisplayName("Case 1: Exact identity across all identifiers -> MATCH")
    void testCase01_ExactIdentityAllMatch() {
        EntityProfileDto left = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .pan("ABCDE1234F")
                .gstin("29ABCDE1234F1Z5")
                .udyamNumber("UDYAM-KA-01-0000001")
                .registeredAddress("12 MG Road Mysuru Karnataka")
                .email("info@abctech.com")
                .phone("9876543210")
                .build();

        EntityProfileDto right = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .pan("ABCDE1234F")
                .gstin("29ABCDE1234F1Z5")
                .udyamNumber("UDYAM-KA-01-0000001")
                .registeredAddress("12 MG Road Mysuru Karnataka")
                .email("info@abctech.com")
                .phone("9876543210")
                .build();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.MATCH);
        assertThat(result.getConfidence()).isEqualTo(1.0);
        assertThat(result.getMatchedAttributes()).contains("PAN", "GSTIN", "UDYAM_NUMBER", "LEGAL_NAME", "REGISTERED_ADDRESS");
        assertThat(result.getMismatchedAttributes()).isEmpty();
        assertThat(result.getExplanation()).contains("Statutory identifier(s) match");
    }

    @Test
    @DisplayName("Case 2: Formatting difference in legal name (Pvt. Ltd. vs Private Limited) with matching PAN -> MATCH")
    void testCase02_FormattingDifferencesLegalName() {
        EntityProfileDto left = EntityProfileDto.builder()
                .legalName("ABC Technologies Pvt. Ltd.")
                .pan("ABCDE1234F")
                .gstin("29ABCDE1234F1Z5")
                .build();

        EntityProfileDto right = EntityProfileDto.builder()
                .legalName("ABC TECHNOLOGIES PRIVATE LIMITED")
                .pan("ABCDE1234F")
                .gstin("29ABCDE1234F1Z5")
                .build();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.MATCH);
        assertThat(result.getConfidence()).isEqualTo(1.0);
        assertThat(result.getMatchedAttributes()).contains("PAN", "GSTIN", "LEGAL_NAME");
        assertThat(result.getMismatchedAttributes()).isEmpty();
    }

    @Test
    @DisplayName("Case 3: Address punctuation differences with matching statutory identifiers -> MATCH")
    void testCase03_AddressPunctuationDifferences() {
        EntityProfileDto left = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .pan("ABCDE1234F")
                .registeredAddress("12, MG Road, Mysuru, Karnataka")
                .build();

        EntityProfileDto right = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .pan("ABCDE1234F")
                .registeredAddress("12 MG ROAD MYSURU KARNATAKA")
                .build();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.MATCH);
        assertThat(result.getMatchedAttributes()).contains("PAN", "LEGAL_NAME", "REGISTERED_ADDRESS");
    }

    @Test
    @DisplayName("Case 4: Strong contradiction - PAN mismatch -> MISMATCH")
    void testCase04_StrongContradictionPanMismatch() {
        EntityProfileDto left = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .pan("ABCDE1234F")
                .build();

        EntityProfileDto right = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .pan("XYZAB9876K")
                .build();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.MISMATCH);
        assertThat(result.getConfidence()).isEqualTo(0.0);
        assertThat(result.getMismatchedAttributes()).contains("PAN");
        assertThat(result.getExplanation()).contains("Hard contradiction detected in statutory identifier(s): PAN");
    }

    @Test
    @DisplayName("Case 5: Strong contradiction - GSTIN mismatch -> MISMATCH")
    void testCase05_StrongContradictionGstinMismatch() {
        EntityProfileDto left = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .gstin("29ABCDE1234F1Z5")
                .build();

        EntityProfileDto right = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .gstin("27XYZAB9876K1Z9")
                .build();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.MISMATCH);
        assertThat(result.getMismatchedAttributes()).contains("GSTIN");
    }

    @Test
    @DisplayName("Case 6: Strong contradiction - Udyam mismatch -> MISMATCH")
    void testCase06_StrongContradictionUdyamMismatch() {
        EntityProfileDto left = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .udyamNumber("UDYAM-KA-01-0000001")
                .build();

        EntityProfileDto right = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .udyamNumber("UDYAM-MH-02-9999999")
                .build();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.MISMATCH);
        assertThat(result.getMismatchedAttributes()).contains("UDYAM_NUMBER");
    }

    @Test
    @DisplayName("Case 7: Missing PAN on one side -> not automatically MISMATCH")
    void testCase07_MissingPanNotMismatch() {
        EntityProfileDto left = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .pan("ABCDE1234F")
                .registeredAddress("12 MG Road Mysuru")
                .build();

        EntityProfileDto right = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .registeredAddress("12 MG Road Mysuru")

                .build();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getMatchStatus()).isNotEqualTo(EntityMatchStatus.MISMATCH);
        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.PROBABLE_MATCH);
        assertThat(result.getMissingAttributes()).contains("PAN");
        assertThat(result.getMismatchedAttributes()).doesNotContain("PAN");
    }

    @Test
    @DisplayName("Case 8: Missing GSTIN on one side -> not automatically MISMATCH")
    void testCase08_MissingGstinNotMismatch() {
        EntityProfileDto left = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .pan("ABCDE1234F")
                .gstin("29ABCDE1234F1Z5")
                .build();

        EntityProfileDto right = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .pan("ABCDE1234F")

                .build();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.MATCH);
        assertThat(result.getMissingAttributes()).contains("GSTIN");
        assertThat(result.getMismatchedAttributes()).isEmpty();
    }

    @Test
    @DisplayName("Case 9: Only legal name available and matching -> PROBABLE_MATCH")
    void testCase09_OnlyLegalNameAvailable() {
        EntityProfileDto left = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .build();

        EntityProfileDto right = EntityProfileDto.builder()
                .legalName("ABC TECHNOLOGIES PVT. LTD.")
                .build();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.PROBABLE_MATCH);
        assertThat(result.getConfidence()).isEqualTo(0.65);
        assertThat(result.getMatchedAttributes()).contains("LEGAL_NAME");
        assertThat(result.getExplanation()).contains("PROBABLE_MATCH: Normalized legal name matches");
    }

    @Test
    @DisplayName("Case 10: No meaningful information -> INSUFFICIENT_DATA")
    void testCase10_NoMeaningfulInformation() {
        EntityProfileDto left = new EntityProfileDto();
        EntityProfileDto right = new EntityProfileDto();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.INSUFFICIENT_DATA);
        assertThat(result.getConfidence()).isEqualTo(0.0);
        assertThat(result.getExplanation()).contains("INSUFFICIENT_DATA");
    }

    @Test
    @DisplayName("Case 11: Name + Address match without statutory identifiers -> PROBABLE_MATCH")
    void testCase11_NameAndAddressMatch() {
        EntityProfileDto left = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .registeredAddress("12 MG Road Mysuru Karnataka")
                .build();

        EntityProfileDto right = EntityProfileDto.builder()
                .legalName("ABC Technologies Pvt Ltd")
                .registeredAddress("12, MG Rd, Mysuru, Karnataka")
                .build();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.PROBABLE_MATCH);
        assertThat(result.getConfidence()).isEqualTo(0.80);
        assertThat(result.getMatchedAttributes()).contains("LEGAL_NAME", "REGISTERED_ADDRESS");
        assertThat(result.getMismatchedAttributes()).isEmpty();
    }

    @Test
    @DisplayName("Case 12: Name matches but phone differs -> do not automatically classify as MISMATCH")
    void testCase12_NameMatchesPhoneDiffers() {
        EntityProfileDto left = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .phone("9876543210")
                .build();

        EntityProfileDto right = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .phone("9123456789")
                .build();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getMatchStatus()).isNotEqualTo(EntityMatchStatus.MISMATCH);
        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.PROBABLE_MATCH);
        assertThat(result.getMismatchedAttributes()).contains("PHONE");
        assertThat(result.getMatchedAttributes()).contains("LEGAL_NAME");
    }

    @Test
    @DisplayName("Case 13: Bidder vs verified evidence with matching PAN -> MATCH")
    void testCase13_BidderVsEvidenceMatchingPan() {
        UUID bidderId = UUID.randomUUID();
        Bidder bidder = Bidder.builder()
                .id(bidderId)
                .legalName("ABC Technologies Pvt. Ltd.")
                .pan("ABCDE1234F")
                .gstin("29ABCDE1234F1Z5")
                .build();

        when(bidderRepository.findById(bidderId)).thenReturn(Optional.of(bidder));

        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder().id(bidId).bidder(bidder).build();
        when(bidRepository.findByBidderId(bidderId)).thenReturn(List.of(bid));

        Evidence panEvidence = Evidence.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .evidenceType(EvidenceType.REGISTRY_RECORD)
                .sourceType(SourceType.PAN)
                .attribute("PAN")
                .value("ABCDE1234F")
                .normalizedValue("ABCDE1234F")
                .verificationStatus(VerificationStatus.VERIFIED)
                .confidence(1.0)
                .observedAt(LocalDateTime.now())
                .build();

        when(evidenceRepository.findByBidId(bidId)).thenReturn(List.of(panEvidence));
        when(claimRepository.findByDocumentBidId(bidId)).thenReturn(Collections.emptyList());

        EntityResolutionResultDto result = entityResolutionService.resolveBidderAgainstEvidence(bidderId);

        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.MATCH);
        assertThat(result.getMatchedAttributes()).contains("PAN");
        verify(entityResolutionResultRepository).save(any(EntityResolutionResult.class));
    }

    @Test
    @DisplayName("Case 14: Bidder vs verified evidence with conflicting PAN -> MISMATCH")
    void testCase14_BidderVsEvidenceConflictingPan() {
        UUID bidderId = UUID.randomUUID();
        Bidder bidder = Bidder.builder()
                .id(bidderId)
                .legalName("ABC Technologies Pvt. Ltd.")
                .pan("ABCDE1234F")
                .build();

        when(bidderRepository.findById(bidderId)).thenReturn(Optional.of(bidder));

        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder().id(bidId).bidder(bidder).build();
        when(bidRepository.findByBidderId(bidderId)).thenReturn(List.of(bid));

        Evidence conflictingPanEvidence = Evidence.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .evidenceType(EvidenceType.REGISTRY_RECORD)
                .sourceType(SourceType.PAN)
                .attribute("PAN")
                .value("XYZAB9876K")
                .normalizedValue("XYZAB9876K")
                .verificationStatus(VerificationStatus.VERIFIED)
                .confidence(1.0)
                .observedAt(LocalDateTime.now())
                .build();

        when(evidenceRepository.findByBidId(bidId)).thenReturn(List.of(conflictingPanEvidence));
        when(claimRepository.findByDocumentBidId(bidId)).thenReturn(Collections.emptyList());

        EntityResolutionResultDto result = entityResolutionService.resolveBidderAgainstEvidence(bidderId);

        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.MISMATCH);
        assertThat(result.getMismatchedAttributes()).contains("PAN");
        verify(evidenceRelationshipRepository).save(any(EvidenceRelationship.class));
    }

    @Test
    @DisplayName("Case 15: Every resolution result contains a deterministic explanation")
    void testCase15_DeterministicExplanationProvided() {
        EntityProfileDto left = EntityProfileDto.builder().legalName("Acme Corp").build();
        EntityProfileDto right = EntityProfileDto.builder().legalName("Acme Corporation").build();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getExplanation()).isNotNull();
        assertThat(result.getExplanation()).isNotBlank();
        assertThat(result.getExplanation()).contains("PROBABLE_MATCH");
    }

    @Test
    @DisplayName("Case 16: Safety rule - Missing evidence is never treated as a contradiction")
    void testCase16_MissingEvidenceNeverTreatedAsContradiction() {
        EntityProfileDto left = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .pan("ABCDE1234F")
                .build();

        EntityProfileDto right = EntityProfileDto.builder()
                .legalName("ABC Technologies Private Limited")
                .build();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getMatchStatus()).isNotEqualTo(EntityMatchStatus.MISMATCH);
        assertThat(result.getMismatchedAttributes()).doesNotContain("PAN");
        assertThat(result.getMissingAttributes()).contains("PAN");
    }

    @Test
    @DisplayName("Case 17: Safety rule - SOURCE_UNAVAILABLE evidence is never treated as verified identity evidence")
    void testCase17_SourceUnavailableNeverTreatedAsVerifiedEvidence() {
        UUID bidderId = UUID.randomUUID();
        Bidder bidder = Bidder.builder()
                .id(bidderId)
                .legalName("ABC Technologies Pvt. Ltd.")
                .pan("ABCDE1234F")
                .build();

        when(bidderRepository.findById(bidderId)).thenReturn(Optional.of(bidder));

        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder().id(bidId).bidder(bidder).build();
        when(bidRepository.findByBidderId(bidderId)).thenReturn(List.of(bid));

        Evidence unavailableEvidence = Evidence.builder()
                .id(UUID.randomUUID())
                .bid(bid)
                .evidenceType(EvidenceType.REGISTRY_RECORD)
                .sourceType(SourceType.PAN)
                .attribute("PAN")
                .value("WRONGPAN12")
                .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                .build();

        when(evidenceRepository.findByBidId(bidId)).thenReturn(List.of(unavailableEvidence));
        when(claimRepository.findByDocumentBidId(bidId)).thenReturn(Collections.emptyList());

        EntityResolutionResultDto result = entityResolutionService.resolveBidderAgainstEvidence(bidderId);

        assertThat(result.getMismatchedAttributes()).doesNotContain("PAN");
        assertThat(result.getMatchStatus()).isNotEqualTo(EntityMatchStatus.MISMATCH);
    }

    @Test
    @DisplayName("Cross check: Embedded PAN in GSTIN mismatch triggers MISMATCH")
    void testEmbeddedPanCrossContradiction() {
        EntityProfileDto left = EntityProfileDto.builder()
                .pan("ABCDE1234F")
                .build();

        EntityProfileDto right = EntityProfileDto.builder()
                .gstin("29XYZAB9876K1Z5")
                .build();

        EntityResolutionResultDto result = entityResolutionService.resolve(left, right);

        assertThat(result.getMatchStatus()).isEqualTo(EntityMatchStatus.MISMATCH);
        assertThat(result.getMismatchedAttributes()).contains("PAN");
    }

}
