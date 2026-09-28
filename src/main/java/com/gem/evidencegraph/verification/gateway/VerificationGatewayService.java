package com.gem.evidencegraph.verification.gateway;

import com.gem.evidencegraph.dto.BulkVerificationResponseDto;
import com.gem.evidencegraph.dto.VerificationRequestDto;
import com.gem.evidencegraph.dto.VerificationResponseDto;
import com.gem.evidencegraph.dto.VerificationResultDto;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.EvidenceRelationship;
import com.gem.evidencegraph.entity.EvidenceType;
import com.gem.evidencegraph.entity.RelationshipType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.VerificationStatus;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.ClaimRepository;
import com.gem.evidencegraph.repository.EvidenceRelationshipRepository;
import com.gem.evidencegraph.repository.EvidenceRepository;
import com.gem.evidencegraph.util.ChecksumUtil;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import com.gem.evidencegraph.verification.VerificationAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class VerificationGatewayService implements VerificationGateway {

    private final ClaimRepository claimRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceRelationshipRepository evidenceRelationshipRepository;
    private final BidRepository bidRepository;
    private final Map<SourceType, VerificationAdapter> adapterRegistry = new EnumMap<>(SourceType.class);

    public VerificationGatewayService(
            ClaimRepository claimRepository,
            EvidenceRepository evidenceRepository,
            EvidenceRelationshipRepository evidenceRelationshipRepository,
            BidRepository bidRepository,
            List<VerificationAdapter> adapters) {
        this.claimRepository = claimRepository;
        this.evidenceRepository = evidenceRepository;
        this.evidenceRelationshipRepository = evidenceRelationshipRepository;
        this.bidRepository = bidRepository;

        for (VerificationAdapter adapter : adapters) {
            this.adapterRegistry.put(adapter.getSourceType(), adapter);
        }
        log.info("Initialized VerificationGatewayService with {} source adapters", adapterRegistry.size());
    }

    @Override
    @Transactional
    public VerificationResponseDto verifyClaim(VerificationRequestDto request) {
        if (request == null || request.getClaimId() == null) {
            throw new IllegalArgumentException("Verification request and claimId must not be null");
        }

        Claim claim = claimRepository.findById(request.getClaimId())
                .orElseThrow(() -> new ResourceNotFoundException("Claim with ID '" + request.getClaimId() + "' not found"));

        SourceType requestedSource = request.getRequestedSourceType();
        if (requestedSource == null) {
            requestedSource = inferSourceType(claim);
        }

        VerificationAdapter adapter = adapterRegistry.get(requestedSource);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported source type: " + requestedSource);
        }

        if (!adapter.supports(claim)) {
            log.warn("Adapter for source {} does not support claim {}", requestedSource, claim.getFieldName());
            return VerificationResponseDto.builder()
                    .claimId(claim.getId())
                    .sourceType(requestedSource)
                    .verificationStatus(VerificationStatus.NOT_APPLICABLE)
                    .expectedValue(claim.getNormalizedValue())
                    .observedValue(null)
                    .confidence(0.0)
                    .reason("Source adapter does not support claim: " + claim.getFieldName())
                    .build();
        }

        VerificationResultDto result = executeAdapterSafely(adapter, claim);

        Optional<Evidence> existingEvidenceOpt = evidenceRepository
                .findByClaimIdAndSourceTypeAndSourceReferenceAndValueAndVerificationStatus(
                        claim.getId(),
                        result.getSourceType(),
                        result.getSourceReference(),
                        result.getObservedValue(),
                        result.getVerificationStatus()
                );

        if (existingEvidenceOpt.isPresent()) {
            Evidence existing = existingEvidenceOpt.get();
            UUID existingRelId = evidenceRelationshipRepository.findBySourceClaimId(claim.getId()).stream()
                    .filter(r -> r.getTargetEvidence() != null && r.getTargetEvidence().getId().equals(existing.getId()))
                    .map(EvidenceRelationship::getId)
                    .findFirst()
                    .orElse(null);

            log.info("Idempotent verification match found for claim {}", claim.getId());
            return VerificationResponseDto.builder()
                    .claimId(claim.getId())
                    .sourceType(existing.getSourceType())
                    .verificationStatus(existing.getVerificationStatus())
                    .expectedValue(result.getExpectedValue())
                    .observedValue(existing.getValue())
                    .evidenceId(existing.getId())
                    .relationshipId(existingRelId)
                    .confidence(existing.getConfidence())
                    .reason("Reused existing verification evidence (Idempotent run)")
                    .build();
        }

        String provenancePayload = String.format("%s:%s:%s:%s",
                result.getSourceType(),
                result.getSourceReference(),
                result.getObservedValue(),
                result.getRawResponseSnapshot() != null ? result.getRawResponseSnapshot() : ""
        );
        String provenanceHash = ChecksumUtil.calculateSha256(provenancePayload.getBytes(StandardCharsets.UTF_8));

        Bid bid = null;
        if (request.getBidId() != null) {
            bid = bidRepository.findById(request.getBidId()).orElse(null);
        }
        if (bid == null && claim.getDocument() != null) {
            bid = claim.getDocument().getBid();
        }

        Evidence evidence = Evidence.builder()
                .bid(bid)
                .claim(claim)
                .evidenceType(EvidenceType.REGISTRY_RECORD)
                .sourceType(result.getSourceType())
                .sourceReference(result.getSourceReference())
                .sourceSystem(result.getSourceSystem())
                .adapterVersion(result.getAdapterVersion())
                .subject(result.getSubject())
                .attribute(result.getAttribute())
                .value(result.getObservedValue())
                .normalizedValue(result.getNormalizedObservedValue())
                .verificationStatus(result.getVerificationStatus())
                .observedAt(result.getObservedAt())
                .validFrom(result.getValidFrom())
                .validUntil(result.getValidUntil())
                .retrievedAt(LocalDateTime.now())
                .confidence(result.getConfidence())
                .provenanceHash(provenanceHash)
                .rawResponseSnapshot(result.getRawResponseSnapshot())
                .build();

        Evidence savedEvidence = evidenceRepository.save(evidence);

        EvidenceRelationship savedRelationship = null;
        if (result.getVerificationStatus() == VerificationStatus.VERIFIED) {
            EvidenceRelationship rel = EvidenceRelationship.builder()
                    .sourceClaim(claim)
                    .targetEvidence(savedEvidence)
                    .relationshipType(RelationshipType.VERIFIED_BY)
                    .reason(result.getReason() != null ? result.getReason() : "Claim verified by external source")
                    .confidence(result.getConfidence())
                    .build();
            savedRelationship = evidenceRelationshipRepository.save(rel);
        } else if (result.getVerificationStatus() == VerificationStatus.MISMATCH) {
            EvidenceRelationship rel = EvidenceRelationship.builder()
                    .sourceClaim(claim)
                    .targetEvidence(savedEvidence)
                    .relationshipType(RelationshipType.CONTRADICTS)
                    .reason(result.getReason() != null ? result.getReason() : "Claim contradicts observed external source evidence")
                    .confidence(result.getConfidence())
                    .build();
            savedRelationship = evidenceRelationshipRepository.save(rel);
        }

        return VerificationResponseDto.builder()
                .claimId(claim.getId())
                .sourceType(savedEvidence.getSourceType())
                .verificationStatus(savedEvidence.getVerificationStatus())
                .expectedValue(result.getExpectedValue())
                .observedValue(savedEvidence.getValue())
                .evidenceId(savedEvidence.getId())
                .relationshipId(savedRelationship != null ? savedRelationship.getId() : null)
                .confidence(savedEvidence.getConfidence())
                .reason(result.getReason())
                .build();
    }

    @Override
    @Transactional
    public BulkVerificationResponseDto verifyBidClaims(UUID bidId, SourceType preferredSourceType) {
        if (bidId == null) {
            throw new IllegalArgumentException("bidId must not be null");
        }

        List<Claim> claims = claimRepository.findByDocumentBidId(bidId);
        List<VerificationResponseDto> results = new ArrayList<>();

        int verifiedCount = 0;
        int mismatchCount = 0;
        int unverifiedCount = 0;
        int unavailableCount = 0;

        for (Claim claim : claims) {
            SourceType targetSource = preferredSourceType != null ? preferredSourceType : inferSourceType(claim);
            if (targetSource != null && adapterRegistry.containsKey(targetSource)) {
                VerificationAdapter adapter = adapterRegistry.get(targetSource);
                if (adapter.supports(claim)) {
                    VerificationRequestDto requestDto = VerificationRequestDto.builder()
                            .claimId(claim.getId())
                            .bidId(bidId)
                            .requestedSourceType(targetSource)
                            .build();
                    VerificationResponseDto resp = verifyClaim(requestDto);
                    results.add(resp);

                    if (resp.getVerificationStatus() == VerificationStatus.VERIFIED) {
                        verifiedCount++;
                    } else if (resp.getVerificationStatus() == VerificationStatus.MISMATCH) {
                        mismatchCount++;
                    } else if (resp.getVerificationStatus() == VerificationStatus.UNVERIFIED) {
                        unverifiedCount++;
                    } else if (resp.getVerificationStatus() == VerificationStatus.SOURCE_UNAVAILABLE) {
                        unavailableCount++;
                    }
                }
            }
        }

        return BulkVerificationResponseDto.builder()
                .bidId(bidId)
                .totalClaimsScanned(claims.size())
                .verifiedCount(verifiedCount)
                .mismatchCount(mismatchCount)
                .unverifiedCount(unverifiedCount)
                .unavailableCount(unavailableCount)
                .results(results)
                .build();
    }

    private VerificationResultDto executeAdapterSafely(VerificationAdapter adapter, Claim claim) {
        try {
            return adapter.verify(claim);
        } catch (Exception e) {
            log.error("Source adapter {} execution failed safely: {}", adapter.getSourceType(), e.getMessage());
            return VerificationResultDto.builder()
                    .sourceType(adapter.getSourceType())
                    .sourceSystem("UNKNOWN")
                    .adapterVersion(adapter.getAdapterVersion())
                    .sourceReference("ERR-EXCEPTION")
                    .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                    .subject(claim.getFieldName())
                    .attribute(claim.getFieldName())
                    .expectedValue(claim.getNormalizedValue())
                    .confidence(0.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot(String.format("{\"error\": \"%s\"}", e.getMessage()))
                    .reason("Source verification adapter failed: " + e.getMessage())
                    .build();
        }
    }

    private SourceType inferSourceType(Claim claim) {
        if (claim == null || claim.getFieldName() == null) {
            return null;
        }
        String field = claim.getFieldName().toUpperCase();
        if ("GSTIN".equals(field)) {
            return SourceType.GST;
        }
        if ("PAN".equals(field)) {
            return SourceType.PAN;
        }
        if ("UDYAM_NUMBER".equals(field) || "UDYAM".equals(field)) {
            return SourceType.UDYAM;
        }
        return null;
    }

}
