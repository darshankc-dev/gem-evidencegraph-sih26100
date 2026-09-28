package com.gem.evidencegraph.risk;

import com.gem.evidencegraph.compliance.ComplianceService;
import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.BidRiskAssessment;
import com.gem.evidencegraph.entity.Bidder;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.EntityResolutionResult;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.EvidenceRelationship;
import com.gem.evidencegraph.entity.RiskAssessment;
import com.gem.evidencegraph.entity.RiskFinding;
import com.gem.evidencegraph.entity.Tender;
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
import com.gem.evidencegraph.risk.evaluator.RiskAssessmentEngine;
import com.gem.evidencegraph.risk.evaluator.RiskEvaluationContext;
import com.gem.evidencegraph.temporal.TemporalService;
import com.gem.evidencegraph.temporal.dto.BidTemporalEvaluationResultDto;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskServiceImpl implements RiskService {

    private final BidRepository bidRepository;
    private final DocumentRepository documentRepository;
    private final ClaimRepository claimRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceRelationshipRepository evidenceRelationshipRepository;
    private final EntityResolutionResultRepository entityResolutionResultRepository;
    private final BidRiskAssessmentRepository bidRiskAssessmentRepository;

    private final ComplianceService complianceService;
    private final TemporalService temporalService;
    private final EntityResolutionService entityResolutionService;
    private final RiskAssessmentEngine riskAssessmentEngine;

    @Override
    @Transactional
    public BidRiskAssessmentResponseDto evaluateBidRisk(UUID bidId) {
        if (bidId == null) {
            throw new IllegalArgumentException("bidId must not be null");
        }

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid with ID '" + bidId + "' not found"));

        Tender tender = bid.getTender();
        Bidder bidder = bid.getBidder();

        List<Document> documents = documentRepository.findByBidId(bidId);

        List<Claim> claims = claimRepository.findByDocumentBidId(bidId);

        Set<Evidence> evidences = new LinkedHashSet<>(evidenceRepository.findByBidId(bidId));
        for (Claim claim : claims) {
            evidences.addAll(evidenceRepository.findByClaimId(claim.getId()));
        }

        BidComplianceResultDto complianceResult = null;
        try {
            complianceResult = complianceService.getLatestBidCompliance(bidId);
        } catch (Exception e) {
            log.warn("Could not retrieve compliance results for bid {}: {}", bidId, e.getMessage());
        }

        BidTemporalEvaluationResultDto temporalResult = null;
        try {
            temporalResult = temporalService.getBidTemporal(bidId);
        } catch (Exception e) {
            log.warn("Could not retrieve temporal results for bid {}: {}", bidId, e.getMessage());
        }

        List<EntityResolutionResult> errList = new ArrayList<>();
        if (bidder != null) {
            errList = entityResolutionResultRepository.findByBidderIdOrderByCreatedAtDesc(bidder.getId());
            if (errList.isEmpty()) {
                try {
                    entityResolutionService.resolveBidderAgainstEvidence(bidder.getId());
                    errList = entityResolutionResultRepository.findByBidderIdOrderByCreatedAtDesc(bidder.getId());
                } catch (Exception e) {
                    log.warn("Could not resolve bidder identity for bidder {}: {}", bidder.getId(), e.getMessage());
                }
            }
        }

        List<EvidenceRelationship> evidenceRelationships = new ArrayList<>();
        for (Evidence ev : evidences) {
            evidenceRelationships.addAll(evidenceRelationshipRepository.findBySourceEvidenceId(ev.getId()));
        }

        LocalDateTime now = LocalDateTime.now();

        RiskEvaluationContext context = RiskEvaluationContext.builder()
                .bid(bid)
                .tender(tender)
                .bidder(bidder)
                .documents(documents)
                .claims(claims)
                .evidences(new ArrayList<>(evidences))
                .complianceResult(complianceResult)
                .temporalResult(temporalResult)
                .entityResolutionResults(errList)
                .evidenceRelationships(evidenceRelationships)
                .evaluatedAt(now)
                .build();

        BidRiskAssessment evaluated = riskAssessmentEngine.evaluate(context);

        Optional<BidRiskAssessment> existingOpt = bidRiskAssessmentRepository.findByBidId(bidId);
        BidRiskAssessment saved;
        if (existingOpt.isPresent()) {
            BidRiskAssessment existing = existingOpt.get();
            existing.setOverallRiskLevel(evaluated.getOverallRiskLevel());
            existing.setOverallRiskScore(evaluated.getOverallRiskScore());
            existing.setDecisionRecommendation(evaluated.getDecisionRecommendation());
            existing.setHardFail(evaluated.getHardFail());
            existing.setSummary(evaluated.getSummary());
            existing.setReason(evaluated.getReason());
            existing.setEvaluatedAt(evaluated.getEvaluatedAt());

            existing.getDimensionAssessments().clear();
            for (RiskAssessment ra : evaluated.getDimensionAssessments()) {
                ra.setBidRiskAssessment(existing);
                ra.setBid(existing.getBid());
                existing.getDimensionAssessments().add(ra);
            }
            saved = bidRiskAssessmentRepository.save(existing);
        } else {
            saved = bidRiskAssessmentRepository.save(evaluated);
        }

        log.info("Persisted risk assessment for bid {}: overallRiskScore={}, overallRiskLevel={}, recommendation={}",
                bid.getBidReference(), saved.getOverallRiskScore(), saved.getOverallRiskLevel(), saved.getDecisionRecommendation());

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BidRiskAssessmentResponseDto getBidRisk(UUID bidId) {
        if (bidId == null) {
            throw new IllegalArgumentException("bidId must not be null");
        }

        if (!bidRepository.existsById(bidId)) {
            throw new ResourceNotFoundException("Bid with ID '" + bidId + "' not found");
        }

        Optional<BidRiskAssessment> stored = bidRiskAssessmentRepository.findFirstByBidIdOrderByEvaluatedAtDesc(bidId);
        if (stored.isPresent()) {
            return mapToDto(stored.get());
        }

        return evaluateBidRisk(bidId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiskDimensionResultDto> getBidRiskDimensions(UUID bidId) {
        BidRiskAssessmentResponseDto report = getBidRisk(bidId);
        return report.getDimensions();
    }

    private BidRiskAssessmentResponseDto mapToDto(BidRiskAssessment entity) {
        List<RiskDimensionResultDto> dimensionDtos = entity.getDimensionAssessments().stream()
                .map(this::mapDimensionToDto)
                .collect(Collectors.toList());

        return BidRiskAssessmentResponseDto.builder()
                .bidId(entity.getBid() != null ? entity.getBid().getId() : null)
                .overallRiskLevel(entity.getOverallRiskLevel())
                .overallRiskScore(entity.getOverallRiskScore())
                .decisionRecommendation(entity.getDecisionRecommendation())
                .hardFail(Boolean.TRUE.equals(entity.getHardFail()))
                .summary(entity.getSummary())
                .reason(entity.getReason())
                .dimensions(dimensionDtos)
                .evaluatedAt(entity.getEvaluatedAt())
                .build();
    }

    private RiskDimensionResultDto mapDimensionToDto(RiskAssessment entity) {
        List<RiskFindingDto> findingDtos = entity.getFindings().stream()
                .map(this::mapFindingToDto)
                .collect(Collectors.toList());

        return RiskDimensionResultDto.builder()
                .dimension(entity.getDimension())
                .riskLevel(entity.getRiskLevel())
                .riskScore(entity.getRiskScore())
                .findingCount(entity.getFindingCount())
                .summary(entity.getSummary())
                .findings(findingDtos)
                .build();
    }

    private RiskFindingDto mapFindingToDto(RiskFinding entity) {
        return RiskFindingDto.builder()
                .dimension(entity.getDimension())
                .severity(entity.getSeverity())
                .code(entity.getCode())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .sourceType(entity.getSourceType())
                .sourceId(entity.getSourceId())
                .rule(entity.getRule())
                .weight(entity.getWeight() != null ? entity.getWeight() : 0)
                .build();
    }

}
