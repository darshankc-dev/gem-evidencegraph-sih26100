package com.gem.evidencegraph.temporal;

import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.TemporalEvaluation;
import com.gem.evidencegraph.entity.Tender;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.ClaimRepository;
import com.gem.evidencegraph.repository.EvidenceRepository;
import com.gem.evidencegraph.repository.TemporalEvaluationRepository;
import com.gem.evidencegraph.temporal.dto.BidTemporalEvaluationResultDto;
import com.gem.evidencegraph.temporal.dto.TemporalEvaluationResponseDto;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
public class TemporalServiceImpl implements TemporalService {

    private final BidRepository bidRepository;
    private final EvidenceRepository evidenceRepository;
    private final ClaimRepository claimRepository;
    private final TemporalEvaluationRepository temporalEvaluationRepository;
    private final TemporalVerificationEngine temporalVerificationEngine;

    @Override
    @Transactional
    public BidTemporalEvaluationResultDto evaluateBidTemporal(UUID bidId) {
        if (bidId == null) {
            throw new IllegalArgumentException("bidId must not be null");
        }

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid with ID '" + bidId + "' not found"));

        Tender tender = bid.getTender();

        List<Claim> claims = claimRepository.findByDocumentBidId(bidId);
        Set<Evidence> evidences = new LinkedHashSet<>(evidenceRepository.findByBidId(bidId));
        for (Claim claim : claims) {
            evidences.addAll(evidenceRepository.findByClaimId(claim.getId()));
        }

        List<TemporalEvaluationResponseDto> responseList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        int validCount = 0;
        int expiredCount = 0;
        int futureValidityCount = 0;
        int issuedAfterBidCount = 0;
        int issuedAfterTenderPublicationCount = 0;
        int conflictingCount = 0;
        int missingTemporalDataCount = 0;
        int notApplicableCount = 0;

        for (Evidence evidence : evidences) {
            TemporalEvaluationContext context = buildContext(bid, tender, evidence, claims);
            TemporalEvaluationOutcome outcome = temporalVerificationEngine.evaluate(context);

            Optional<TemporalEvaluation> existingOpt = temporalEvaluationRepository.findByBidIdAndEvidenceId(
                    bid.getId(), evidence.getId());

            TemporalEvaluation evaluation;
            if (existingOpt.isPresent()) {
                evaluation = existingOpt.get();
                evaluation.setReferenceDate(context.getBidSubmissionDate());
                evaluation.setValidFrom(context.getValidFrom());
                evaluation.setValidUntil(context.getValidUntil());
                evaluation.setObservedAt(context.getObservedAt());
                evaluation.setIssueDate(context.getIssueDate());
                evaluation.setStatus(outcome.getPrimaryStatus());
                evaluation.setReason(outcome.getReason());
                evaluation.setRuleApplied(outcome.getRuleApplied());
                evaluation.setEvaluatedAt(now);
            } else {
                evaluation = TemporalEvaluation.builder()
                        .bid(bid)
                        .tender(tender)
                        .evidence(evidence)
                        .referenceDate(context.getBidSubmissionDate())
                        .validFrom(context.getValidFrom())
                        .validUntil(context.getValidUntil())
                        .observedAt(context.getObservedAt())
                        .issueDate(context.getIssueDate())
                        .status(outcome.getPrimaryStatus())
                        .reason(outcome.getReason())
                        .ruleApplied(outcome.getRuleApplied())
                        .evaluatedAt(now)
                        .build();
            }

            TemporalEvaluation saved = temporalEvaluationRepository.save(evaluation);
            responseList.add(mapToDto(saved));

            switch (outcome.getPrimaryStatus()) {
                case VALID_AT_BID_DATE -> validCount++;
                case EXPIRED_AT_BID_DATE -> expiredCount++;
                case NOT_YET_VALID_AT_BID_DATE -> futureValidityCount++;
                case ISSUED_AFTER_BID -> issuedAfterBidCount++;
                case ISSUED_AFTER_TENDER_PUBLICATION -> issuedAfterTenderPublicationCount++;
                case CONFLICTING_DATES -> conflictingCount++;
                case MISSING_TEMPORAL_DATA -> missingTemporalDataCount++;
                case NOT_APPLICABLE -> notApplicableCount++;
            }
        }

        log.info("Completed temporal evaluation for bid {}: total={}, valid={}, expired={}, conflicting={}",
                bid.getBidReference(), evidences.size(), validCount, expiredCount, conflictingCount);

        return BidTemporalEvaluationResultDto.builder()
                .bidId(bid.getId())
                .tenderId(tender != null ? tender.getId() : null)
                .evaluatedAt(now)
                .totalEvaluated(responseList.size())
                .validCount(validCount)
                .expiredCount(expiredCount)
                .futureValidityCount(futureValidityCount)
                .issuedAfterBidCount(issuedAfterBidCount)
                .issuedAfterTenderPublicationCount(issuedAfterTenderPublicationCount)
                .conflictingCount(conflictingCount)
                .missingTemporalDataCount(missingTemporalDataCount)
                .notApplicableCount(notApplicableCount)
                .results(responseList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BidTemporalEvaluationResultDto getBidTemporal(UUID bidId) {
        if (bidId == null) {
            throw new IllegalArgumentException("bidId must not be null");
        }

        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid with ID '" + bidId + "' not found"));

        List<TemporalEvaluation> stored = temporalEvaluationRepository.findByBidIdOrderByEvaluatedAtDesc(bidId);
        if (stored.isEmpty()) {
            return evaluateBidTemporal(bidId);
        }

        List<TemporalEvaluationResponseDto> results = stored.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        int validCount = 0;
        int expiredCount = 0;
        int futureValidityCount = 0;
        int issuedAfterBidCount = 0;
        int issuedAfterTenderPublicationCount = 0;
        int conflictingCount = 0;
        int missingTemporalDataCount = 0;
        int notApplicableCount = 0;

        for (TemporalEvaluationResponseDto r : results) {
            if (r.getStatus() != null) {
                switch (r.getStatus()) {
                    case VALID_AT_BID_DATE -> validCount++;
                    case EXPIRED_AT_BID_DATE -> expiredCount++;
                    case NOT_YET_VALID_AT_BID_DATE -> futureValidityCount++;
                    case ISSUED_AFTER_BID -> issuedAfterBidCount++;
                    case ISSUED_AFTER_TENDER_PUBLICATION -> issuedAfterTenderPublicationCount++;
                    case CONFLICTING_DATES -> conflictingCount++;
                    case MISSING_TEMPORAL_DATA -> missingTemporalDataCount++;
                    case NOT_APPLICABLE -> notApplicableCount++;
                }
            }
        }

        LocalDateTime latestEvaluatedAt = stored.get(0).getEvaluatedAt();

        return BidTemporalEvaluationResultDto.builder()
                .bidId(bid.getId())
                .tenderId(bid.getTender() != null ? bid.getTender().getId() : null)
                .evaluatedAt(latestEvaluatedAt)
                .totalEvaluated(results.size())
                .validCount(validCount)
                .expiredCount(expiredCount)
                .futureValidityCount(futureValidityCount)
                .issuedAfterBidCount(issuedAfterBidCount)
                .issuedAfterTenderPublicationCount(issuedAfterTenderPublicationCount)
                .conflictingCount(conflictingCount)
                .missingTemporalDataCount(missingTemporalDataCount)
                .notApplicableCount(notApplicableCount)
                .results(results)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemporalEvaluationResponseDto> getEvidenceTemporal(UUID evidenceId) {
        if (evidenceId == null) {
            throw new IllegalArgumentException("evidenceId must not be null");
        }

        if (!evidenceRepository.existsById(evidenceId)) {
            throw new ResourceNotFoundException("Evidence with ID '" + evidenceId + "' not found");
        }

        return temporalEvaluationRepository.findByEvidenceIdOrderByEvaluatedAtDesc(evidenceId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private TemporalEvaluationContext buildContext(Bid bid, Tender tender, Evidence evidence, List<Claim> allBidClaims) {
        LocalDateTime issueDate = resolveIssueDate(evidence, allBidClaims);

        Boolean requiresTemporal = null;
        if (evidence.getSourceType() == SourceType.DEBARMENT_LIST && evidence.getValidFrom() == null && evidence.getValidUntil() == null) {

            requiresTemporal = false;
        }

        return TemporalEvaluationContext.builder()
                .bidId(bid.getId())
                .tenderId(tender != null ? tender.getId() : null)
                .bidSubmissionDate(bid.getSubmissionDate())
                .tenderPublicationDate(tender != null ? tender.getPublicationDate() : null)
                .tenderSubmissionDeadline(tender != null ? tender.getSubmissionDeadline() : null)
                .evidenceId(evidence.getId())
                .evidenceType(evidence.getEvidenceType())
                .sourceType(evidence.getSourceType())
                .subject(evidence.getSubject())
                .attribute(evidence.getAttribute())
                .observedAt(evidence.getObservedAt())
                .validFrom(evidence.getValidFrom())
                .validUntil(evidence.getValidUntil())
                .issueDate(issueDate)
                .requiresTemporalEvaluation(requiresTemporal)
                .build();
    }

    private LocalDateTime resolveIssueDate(Evidence evidence, List<Claim> allBidClaims) {

        if (evidence.getClaim() != null) {
            Claim claim = evidence.getClaim();
            if (isIssueDateClaimField(claim.getFieldName())) {
                LocalDateTime parsed = parseDateTime(claim.getNormalizedValue());
                if (parsed != null) {
                    return parsed;
                }
            }

            if (claim.getDocument() != null && allBidClaims != null && isEligibleForDocumentRegistrationDate(evidence, claim)) {
                UUID docId = claim.getDocument().getId();
                for (Claim c : allBidClaims) {
                    if (c.getDocument() != null && docId.equals(c.getDocument().getId())) {
                        if (isIssueDateClaimField(c.getFieldName())) {
                            LocalDateTime parsed = parseDateTime(c.getNormalizedValue());
                            if (parsed != null) {
                                return parsed;
                            }
                        }
                    }
                }
            }
        }

        if (isIssueDateClaimField(evidence.getAttribute()) || isIssueDateClaimField(evidence.getSubject())) {
            LocalDateTime parsed = parseDateTime(evidence.getNormalizedValue() != null ? evidence.getNormalizedValue() : evidence.getValue());
            if (parsed != null) {
                return parsed;
            }
        }

        return null;
    }

    private boolean isEligibleForDocumentRegistrationDate(Evidence evidence, Claim claim) {
        if (evidence == null || claim == null || claim.getDocument() == null) {
            return false;
        }

        if (evidence.getSourceType() == SourceType.DEBARMENT_LIST) {
            return false;
        }

        String attr = evidence.getAttribute() != null ? evidence.getAttribute().toUpperCase() : "";
        String subj = evidence.getSubject() != null ? evidence.getSubject().toUpperCase() : "";
        if (attr.contains("DEBAR") || attr.contains("BLACKLIST") || subj.contains("DEBAR") || subj.contains("BLACKLIST")
                || attr.contains("DECLARATION") || subj.contains("DECLARATION")
                || attr.contains("AFFIDAVIT") || subj.contains("AFFIDAVIT")) {
            return false;
        }

        Document document = claim.getDocument();
        DocumentType docType = document.getDocumentType();
        if (docType == DocumentType.BLACKLIST_DECLARATION || docType == DocumentType.OTHER) {
            return false;
        }

        boolean isRegistrationDocument = docType == DocumentType.GST_CERTIFICATE
                || docType == DocumentType.UDYAM_CERTIFICATE
                || docType == DocumentType.OEM_AUTHORIZATION
                || docType == DocumentType.PAN_DOCUMENT
                || docType == DocumentType.STARTUP_CERTIFICATE
                || docType == DocumentType.NSIC_CERTIFICATE
                || docType == DocumentType.EPFO_DOCUMENT
                || docType == DocumentType.ESIC_DOCUMENT;

        if (!isRegistrationDocument) {
            return false;
        }

        if (evidence.getSourceType() == SourceType.PAN && docType != DocumentType.PAN_DOCUMENT) {
            return false;
        }

        String claimField = claim.getFieldName() != null ? claim.getFieldName().toUpperCase() : "";
        return claimField.contains("GST")
                || claimField.contains("UDYAM")
                || claimField.contains("OEM")
                || claimField.contains("PAN")
                || claimField.contains("CERTIFICATE")
                || claimField.contains("LICENSE")
                || claimField.contains("REGISTRATION");
    }

    private boolean isIssueDateClaimField(String field) {
        if (field == null) {
            return false;
        }
        String upper = field.toUpperCase();
        return upper.contains("REGISTRATION_DATE")
                || upper.contains("ISSUE_DATE")
                || upper.contains("DATE_OF_ISSUE")
                || upper.contains("DATE_OF_REGISTRATION")
                || upper.contains("DATE_OF_INCORPORATION");
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception ignored2) {
                return null;
            }
        }
    }

    private TemporalEvaluationResponseDto mapToDto(TemporalEvaluation evaluation) {
        return TemporalEvaluationResponseDto.builder()
                .evaluationId(evaluation.getId())
                .bidId(evaluation.getBid() != null ? evaluation.getBid().getId() : null)
                .tenderId(evaluation.getTender() != null ? evaluation.getTender().getId() : null)
                .evidenceId(evidenceId(evaluation))
                .referenceDate(evaluation.getReferenceDate())
                .validFrom(evaluation.getValidFrom())
                .validUntil(evaluation.getValidUntil())
                .observedAt(evaluation.getObservedAt())
                .issueDate(evaluation.getIssueDate())
                .status(evaluation.getStatus())
                .reason(evaluation.getReason())
                .evaluatedAt(evaluation.getEvaluatedAt())
                .build();
    }

    private UUID evidenceId(TemporalEvaluation evaluation) {
        return evaluation.getEvidence() != null ? evaluation.getEvidence().getId() : null;
    }

}
