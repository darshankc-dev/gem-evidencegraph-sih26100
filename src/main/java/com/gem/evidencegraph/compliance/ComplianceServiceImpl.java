package com.gem.evidencegraph.compliance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.compliance.dto.CreateRequirementRequestDto;
import com.gem.evidencegraph.compliance.dto.RequirementResponseDto;
import com.gem.evidencegraph.compliance.rule.BidEvaluationContext;
import com.gem.evidencegraph.compliance.rule.ComplianceRuleEngine;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.BidComplianceEvaluation;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.ComplianceRequirement;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.Tender;
import com.gem.evidencegraph.repository.BidComplianceEvaluationRepository;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.ClaimRepository;
import com.gem.evidencegraph.repository.ComplianceRequirementRepository;
import com.gem.evidencegraph.repository.DocumentRepository;
import com.gem.evidencegraph.repository.EvidenceRepository;
import com.gem.evidencegraph.repository.TenderRepository;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class ComplianceServiceImpl implements ComplianceService {

    private final TenderRepository tenderRepository;
    private final BidRepository bidRepository;
    private final ComplianceRequirementRepository requirementRepository;
    private final DocumentRepository documentRepository;
    private final ClaimRepository claimRepository;
    private final EvidenceRepository evidenceRepository;
    private final BidComplianceEvaluationRepository evaluationRepository;
    private final ComplianceRuleEngine complianceRuleEngine;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public RequirementResponseDto createRequirement(UUID tenderId, CreateRequirementRequestDto request) {
        if (tenderId == null) {
            throw new IllegalArgumentException("tenderId must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Requirement request must not be null");
        }

        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResourceNotFoundException("Tender with ID '" + tenderId + "' not found"));

        Optional<ComplianceRequirement> existingOpt = requirementRepository.findByTenderIdAndRequirementCode(
                tenderId, request.getRequirementCode());

        ComplianceRequirement requirement;
        if (existingOpt.isPresent()) {
            requirement = existingOpt.get();
            requirement.setName(request.getName());
            requirement.setDescription(request.getDescription());
            requirement.setRequirementType(request.getRequirementType());
            requirement.setMandatory(request.isMandatory());
            requirement.setExpectedSourceType(request.getExpectedSourceType());
            requirement.setExpectedDocumentType(request.getExpectedDocumentType());
            requirement.setValidationRule(request.getValidationRule());
        } else {
            requirement = ComplianceRequirement.builder()
                    .tender(tender)
                    .requirementCode(request.getRequirementCode())
                    .name(request.getName())
                    .description(request.getDescription())
                    .requirementType(request.getRequirementType())
                    .mandatory(request.isMandatory())
                    .expectedSourceType(request.getExpectedSourceType())
                    .expectedDocumentType(request.getExpectedDocumentType())
                    .validationRule(request.getValidationRule())
                    .build();
        }

        ComplianceRequirement saved = requirementRepository.save(requirement);
        log.info("Saved compliance requirement '{}' for tender '{}'", saved.getRequirementCode(), tender.getTenderReference());
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequirementResponseDto> getRequirementsByTender(UUID tenderId) {
        if (!tenderRepository.existsById(tenderId)) {
            throw new ResourceNotFoundException("Tender with ID '" + tenderId + "' not found");
        }
        return requirementRepository.findByTenderId(tenderId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RequirementResponseDto getRequirementById(UUID requirementId) {
        ComplianceRequirement req = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement with ID '" + requirementId + "' not found"));
        return mapToDto(req);
    }

    @Override
    @Transactional
    public BidComplianceResultDto evaluateBidCompliance(UUID bidId) {
        if (bidId == null) {
            throw new IllegalArgumentException("bidId must not be null");
        }

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid with ID '" + bidId + "' not found"));

        Tender tender = bid.getTender();
        List<ComplianceRequirement> requirements = requirementRepository.findByTenderId(tender.getId());

        List<Document> documents = documentRepository.findByBidId(bidId);

        List<Claim> claims = claimRepository.findByDocumentBidId(bidId);

        Set<Evidence> evidences = new LinkedHashSet<>(evidenceRepository.findByBidId(bidId));
        for (Claim claim : claims) {
            evidences.addAll(evidenceRepository.findByClaimId(claim.getId()));
        }

        BidEvaluationContext context = BidEvaluationContext.builder()
                .bid(bid)
                .bidder(bid.getBidder())
                .tender(tender)
                .documents(documents)
                .claims(claims)
                .evidences(new ArrayList<>(evidences))
                .build();

        BidComplianceResultDto resultDto = complianceRuleEngine.evaluate(context, requirements);

        String detailsJson = null;
        try {
            detailsJson = objectMapper.writeValueAsString(resultDto.getRequirementResults());
        } catch (Exception e) {
            log.warn("Failed to serialize compliance requirement results to JSON: {}", e.getMessage());
        }

        BidComplianceEvaluation auditRecord = BidComplianceEvaluation.builder()
                .bid(bid)
                .overallStatus(resultDto.getOverallStatus())
                .compliancePercentage(resultDto.getCompliancePercentage())
                .mandatoryRequirementCount(resultDto.getMandatoryRequirementCount())
                .compliantCount(resultDto.getCompliantCount())
                .nonCompliantCount(resultDto.getNonCompliantCount())
                .missingCount(resultDto.getMissingCount())
                .unverifiedCount(resultDto.getUnverifiedCount())
                .contradictoryCount(resultDto.getContradictoryCount())
                .pendingReviewCount(resultDto.getPendingReviewCount())
                .notApplicableCount(resultDto.getNotApplicableCount())
                .evaluationDetailsJson(detailsJson)
                .summary(resultDto.getSummary())
                .build();

        evaluationRepository.save(auditRecord);
        log.info("Persisted compliance evaluation audit record for bid {}", bid.getBidReference());

        return resultDto;
    }

    @Override
    @Transactional(readOnly = true)
    public BidComplianceResultDto getLatestBidCompliance(UUID bidId) {
        if (bidId == null) {
            throw new IllegalArgumentException("bidId must not be null");
        }

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid with ID '" + bidId + "' not found"));

        Optional<BidComplianceEvaluation> latestOpt = evaluationRepository.findFirstByBidIdOrderByCreatedAtDesc(bidId);
        if (latestOpt.isPresent()) {
            BidComplianceEvaluation eval = latestOpt.get();
            List<ComplianceEvaluationResultDto> items = new ArrayList<>();
            if (eval.getEvaluationDetailsJson() != null) {
                try {
                    items = objectMapper.readValue(eval.getEvaluationDetailsJson(), new TypeReference<>() {});
                } catch (Exception e) {
                    log.warn("Could not deserialize evaluation details JSON: {}", e.getMessage());
                }
            }

            return BidComplianceResultDto.builder()
                    .bidId(bid.getId())
                    .tenderId(bid.getTender() != null ? bid.getTender().getId() : null)
                    .bidderId(bid.getBidder() != null ? bid.getBidder().getId() : null)
                    .overallStatus(eval.getOverallStatus())
                    .compliancePercentage(eval.getCompliancePercentage())
                    .mandatoryRequirementCount(eval.getMandatoryRequirementCount())
                    .compliantCount(eval.getCompliantCount())
                    .nonCompliantCount(eval.getNonCompliantCount())
                    .missingCount(eval.getMissingCount())
                    .unverifiedCount(eval.getUnverifiedCount())
                    .contradictoryCount(eval.getContradictoryCount())
                    .pendingReviewCount(eval.getPendingReviewCount())
                    .notApplicableCount(eval.getNotApplicableCount())
                    .requirementResults(items)
                    .summary(eval.getSummary())
                    .evaluatedAt(eval.getCreatedAt())
                    .build();
        }

        return evaluateBidCompliance(bidId);
    }

    private RequirementResponseDto mapToDto(ComplianceRequirement req) {
        return RequirementResponseDto.builder()
                .id(req.getId())
                .tenderId(req.getTender() != null ? req.getTender().getId() : null)
                .requirementCode(req.getRequirementCode())
                .name(req.getName())
                .description(req.getDescription())
                .requirementType(req.getRequirementType())
                .mandatory(req.isMandatory())
                .expectedSourceType(req.getExpectedSourceType())
                .expectedDocumentType(req.getExpectedDocumentType())
                .validationRule(req.getValidationRule())
                .createdAt(req.getCreatedAt())
                .build();
    }

}
