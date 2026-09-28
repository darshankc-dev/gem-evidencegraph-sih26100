package com.gem.evidencegraph.controller;

import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.BidStatus;
import com.gem.evidencegraph.entity.Bidder;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.EvidenceRelationship;
import com.gem.evidencegraph.entity.EvidenceType;
import com.gem.evidencegraph.entity.RelationshipType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.Tender;
import com.gem.evidencegraph.entity.VerificationStatus;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.EvidenceRelationshipRepository;
import com.gem.evidencegraph.repository.EvidenceRepository;
import com.gem.evidencegraph.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BidDetailsController.class)
@Import(SecurityConfig.class)
class BidDetailsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BidRepository bidRepository;

    @MockBean
    private EvidenceRepository evidenceRepository;

    @MockBean
    private EvidenceRelationshipRepository evidenceRelationshipRepository;

    @Test
    @DisplayName("GET /api/bids/{bidId} returns 200 with bid overview when found")
    void shouldReturnBidOverviewSuccessfully() throws Exception {
        UUID bidId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        UUID tenderId = UUID.randomUUID();

        Bidder bidder = Bidder.builder()
                .id(bidderId)
                .legalName("Acme Security Systems Pvt Ltd")
                .build();

        Tender tender = Tender.builder()
                .id(tenderId)
                .tenderReference("GEM/2026/B/88219")
                .title("CCTV Surveillance Supply")
                .build();

        Bid bid = Bid.builder()
                .id(bidId)
                .bidReference("BID-2026-9901")
                .bidder(bidder)
                .tender(tender)
                .submissionDate(LocalDateTime.of(2026, 9, 20, 10, 30))
                .status(BidStatus.SUBMITTED)
                .build();

        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

        mockMvc.perform(get("/api/bids/" + bidId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bidId").value(bidId.toString()))
                .andExpect(jsonPath("$.bidReference").value("BID-2026-9901"))
                .andExpect(jsonPath("$.bidderLegalName").value("Acme Security Systems Pvt Ltd"))
                .andExpect(jsonPath("$.tenderReference").value("GEM/2026/B/88219"))
                .andExpect(jsonPath("$.tenderTitle").value("CCTV Surveillance Supply"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("GET /api/bids/{bidId} returns 404 when bid is not found")
    void shouldReturn404WhenBidNotFound() throws Exception {
        UUID bidId = UUID.randomUUID();
        when(bidRepository.findById(bidId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/bids/" + bidId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/bids/{bidId}/evidence returns 200 with evidence records and relationships")
    void shouldReturnEvidenceGraphSuccessfully() throws Exception {
        UUID bidId = UUID.randomUUID();
        UUID evidenceId = UUID.randomUUID();
        UUID relId = UUID.randomUUID();

        Evidence evidence = Evidence.builder()
                .id(evidenceId)
                .evidenceType(EvidenceType.REGISTRY_RECORD)
                .sourceType(SourceType.GST)
                .attribute("GSTIN")
                .value("07AAAAA0000A1Z5")
                .verificationStatus(VerificationStatus.VERIFIED)
                .confidence(1.0)
                .build();

        EvidenceRelationship rel = EvidenceRelationship.builder()
                .id(relId)
                .relationshipType(RelationshipType.VERIFIED_BY)
                .reason("GST active registry record match")
                .confidence(1.0)
                .build();

        when(evidenceRepository.findByBidId(bidId)).thenReturn(List.of(evidence));
        when(evidenceRelationshipRepository.findByTargetEvidenceId(evidenceId)).thenReturn(List.of(rel));

        mockMvc.perform(get("/api/bids/" + bidId + "/evidence")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(evidenceId.toString()))
                .andExpect(jsonPath("$[0].sourceType").value("GST"))
                .andExpect(jsonPath("$[0].attribute").value("GSTIN"))
                .andExpect(jsonPath("$[0].verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$[0].relationships[0].relationshipType").value("VERIFIED_BY"));
    }

    @Test
    @DisplayName("GET /api/bids returns 200 with list of all bids")
    void shouldReturnAllBidsSuccessfully() throws Exception {
        UUID bidId = UUID.randomUUID();
        Bidder bidder = Bidder.builder()
                .id(UUID.randomUUID())
                .legalName("Apex Secure Systems Pvt Ltd")
                .build();
        Tender tender = Tender.builder()
                .id(UUID.randomUUID())
                .tenderReference("GEM-DEMO-2026-001")
                .title("Network Security Equipment")
                .build();
        Bid bid = Bid.builder()
                .id(bidId)
                .bidReference("GEM-DEMO-BID-001")
                .status(BidStatus.SUBMITTED)
                .submissionDate(LocalDateTime.now())
                .bidder(bidder)
                .tender(tender)
                .build();

        when(bidRepository.findAll()).thenReturn(List.of(bid));

        mockMvc.perform(get("/api/bids")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bidId").value(bidId.toString()))
                .andExpect(jsonPath("$[0].bidReference").value("GEM-DEMO-BID-001"))
                .andExpect(jsonPath("$[0].bidderLegalName").value("Apex Secure Systems Pvt Ltd"));
    }

}
