package com.gem.evidencegraph.controller;

import com.gem.evidencegraph.dto.BidOverviewDto;
import com.gem.evidencegraph.dto.EvidenceGraphResponseDto;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.EvidenceRelationship;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.EvidenceRelationshipRepository;
import com.gem.evidencegraph.repository.EvidenceRepository;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidDetailsController {

    private final BidRepository bidRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceRelationshipRepository evidenceRelationshipRepository;

    @GetMapping("/bids")
    public ResponseEntity<List<BidOverviewDto>> getAllBids() {
        log.info("Fetching overview of all bids");
        List<Bid> bids = bidRepository.findAll();
        List<BidOverviewDto> results = bids.stream()
                .map(BidOverviewDto::fromEntity)
                .toList();
        return ResponseEntity.ok(results);
    }

    @GetMapping("/bids/{bidId}")
    public ResponseEntity<BidOverviewDto> getBidOverview(@PathVariable UUID bidId) {
        log.info("Fetching bid overview for ID: {}", bidId);
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found with ID: " + bidId));
        return ResponseEntity.ok(BidOverviewDto.fromEntity(bid));
    }

    @GetMapping("/bids/{bidId}/evidence")
    public ResponseEntity<List<EvidenceGraphResponseDto>> getBidEvidenceGraph(@PathVariable UUID bidId) {
        log.info("Fetching evidence graph for bid ID: {}", bidId);
        List<Evidence> evidences = evidenceRepository.findByBidId(bidId);
        List<EvidenceGraphResponseDto> results = new ArrayList<>();

        for (Evidence ev : evidences) {
            List<EvidenceRelationship> relationships = evidenceRelationshipRepository.findByTargetEvidenceId(ev.getId());
            List<EvidenceGraphResponseDto.RelationshipItemDto> relDtos = new ArrayList<>();
            for (EvidenceRelationship r : relationships) {
                relDtos.add(EvidenceGraphResponseDto.RelationshipItemDto.builder()
                        .relationshipId(r.getId())
                        .sourceClaimId(r.getSourceClaim() != null ? r.getSourceClaim().getId() : null)
                        .sourceEvidenceId(r.getSourceEvidence() != null ? r.getSourceEvidence().getId() : null)
                        .targetEvidenceId(ev.getId())
                        .relationshipType(r.getRelationshipType())
                        .reason(r.getReason())
                        .confidence(r.getConfidence())
                        .build());
            }

            results.add(EvidenceGraphResponseDto.builder()
                    .id(ev.getId())
                    .bidId(bidId)
                    .claimId(ev.getClaim() != null ? ev.getClaim().getId() : null)
                    .evidenceType(ev.getEvidenceType())
                    .sourceType(ev.getSourceType())
                    .sourceReference(ev.getSourceReference())
                    .sourceSystem(ev.getSourceSystem())
                    .adapterVersion(ev.getAdapterVersion())
                    .subject(ev.getSubject())
                    .attribute(ev.getAttribute())
                    .value(ev.getValue())
                    .normalizedValue(ev.getNormalizedValue())
                    .verificationStatus(ev.getVerificationStatus())
                    .observedAt(ev.getObservedAt())
                    .validFrom(ev.getValidFrom())
                    .validUntil(ev.getValidUntil())
                    .retrievedAt(ev.getRetrievedAt())
                    .confidence(ev.getConfidence())
                    .provenanceHash(ev.getProvenanceHash())
                    .relationships(relDtos)
                    .build());
        }

        return ResponseEntity.ok(results);
    }

}
