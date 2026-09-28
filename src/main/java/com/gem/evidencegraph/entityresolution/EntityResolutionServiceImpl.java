package com.gem.evidencegraph.entityresolution;

import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Bidder;
import com.gem.evidencegraph.entity.EntityResolutionResult;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.EvidenceRelationship;
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
import com.gem.evidencegraph.util.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EntityResolutionServiceImpl implements EntityResolutionService {

    private final EntityNormalizationService normalizationService;
    private final BidderRepository bidderRepository;
    private final BidRepository bidRepository;
    private final ClaimRepository claimRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceRelationshipRepository evidenceRelationshipRepository;
    private final EntityResolutionResultRepository entityResolutionResultRepository;

    public EntityResolutionServiceImpl(
            EntityNormalizationService normalizationService,
            BidderRepository bidderRepository,
            BidRepository bidRepository,
            ClaimRepository claimRepository,
            EvidenceRepository evidenceRepository,
            EvidenceRelationshipRepository evidenceRelationshipRepository,
            EntityResolutionResultRepository entityResolutionResultRepository) {
        this.normalizationService = normalizationService;
        this.bidderRepository = bidderRepository;
        this.bidRepository = bidRepository;
        this.claimRepository = claimRepository;
        this.evidenceRepository = evidenceRepository;
        this.evidenceRelationshipRepository = evidenceRelationshipRepository;
        this.entityResolutionResultRepository = entityResolutionResultRepository;
    }

    @Override
    public EntityResolutionResultDto resolve(EntityProfileDto rawLeft, EntityProfileDto rawRight) {
        if (rawLeft == null && rawRight == null) {
            return EntityResolutionResultDto.builder()
                    .matchStatus(EntityMatchStatus.INSUFFICIENT_DATA)
                    .confidence(0.0)
                    .explanation("Both left and right entity profiles are null.")
                    .build();
        }

        EntityProfileDto left = normalizationService.normalizeProfile(rawLeft != null ? rawLeft : new EntityProfileDto());
        EntityProfileDto right = normalizationService.normalizeProfile(rawRight != null ? rawRight : new EntityProfileDto());

        List<String> comparedAttributes = new ArrayList<>();
        List<String> matchedAttributes = new ArrayList<>();
        List<String> mismatchedAttributes = new ArrayList<>();
        List<String> missingAttributes = new ArrayList<>();
        List<String> explanations = new ArrayList<>();

        compareStrongIdentifier("PAN", left.getPan(), right.getPan(),
                comparedAttributes, matchedAttributes, mismatchedAttributes, missingAttributes, explanations);

        compareStrongIdentifier("GSTIN", left.getGstin(), right.getGstin(),
                comparedAttributes, matchedAttributes, mismatchedAttributes, missingAttributes, explanations);

        compareStrongIdentifier("UDYAM_NUMBER", left.getUdyamNumber(), right.getUdyamNumber(),
                comparedAttributes, matchedAttributes, mismatchedAttributes, missingAttributes, explanations);

        checkEmbeddedPanCrossContradiction(left, right, comparedAttributes, mismatchedAttributes, explanations);

        compareLegalName(left.getLegalName(), right.getLegalName(),
                comparedAttributes, matchedAttributes, mismatchedAttributes, missingAttributes, explanations);

        compareAddress(left.getRegisteredAddress(), right.getRegisteredAddress(),
                comparedAttributes, matchedAttributes, mismatchedAttributes, missingAttributes, explanations);

        compareContact("EMAIL", left.getEmail(), right.getEmail(),
                comparedAttributes, matchedAttributes, mismatchedAttributes, missingAttributes, explanations);

        compareContact("PHONE", left.getPhone(), right.getPhone(),
                comparedAttributes, matchedAttributes, mismatchedAttributes, missingAttributes, explanations);

        return evaluateDecision(comparedAttributes, matchedAttributes, mismatchedAttributes, missingAttributes, explanations);
    }

    @Override
    @Transactional
    public EntityResolutionResultDto resolveBidderAgainstEvidence(UUID bidderId) {
        if (bidderId == null) {
            throw new IllegalArgumentException("bidderId must not be null");
        }

        Bidder bidder = bidderRepository.findById(bidderId)
                .orElseThrow(() -> new ResourceNotFoundException("Bidder with ID '" + bidderId + "' not found"));

        EntityProfileDto bidderProfile = EntityProfileDto.builder()
                .legalName(bidder.getLegalName())
                .pan(bidder.getPan())
                .gstin(bidder.getGstin())
                .udyamNumber(bidder.getUdyamNumber())
                .registeredAddress(bidder.getRegisteredAddress())
                .email(bidder.getEmail())
                .phone(bidder.getPhone())
                .build();

        List<Bid> bids = bidRepository.findByBidderId(bidderId);
        Set<Evidence> verifiedEvidences = new LinkedHashSet<>();

        for (Bid bid : bids) {

            List<Evidence> bidEvidences = evidenceRepository.findByBidId(bid.getId());
            for (Evidence ev : bidEvidences) {
                if (ev.getVerificationStatus() == VerificationStatus.VERIFIED) {
                    verifiedEvidences.add(ev);
                }
            }

            claimRepository.findByDocumentBidId(bid.getId()).forEach(claim -> {
                evidenceRepository.findByClaimId(claim.getId()).forEach(ev -> {
                    if (ev.getVerificationStatus() == VerificationStatus.VERIFIED) {
                        verifiedEvidences.add(ev);
                    }
                });
            });
        }

        EntityProfileDto evidenceProfile = new EntityProfileDto();
        for (Evidence ev : verifiedEvidences) {
            String val = ev.getNormalizedValue() != null ? ev.getNormalizedValue() : ev.getValue();
            if (val == null || val.isBlank()) {
                continue;
            }

            if (ev.getSourceType() == SourceType.PAN || "PAN".equalsIgnoreCase(ev.getAttribute()) || "PAN".equalsIgnoreCase(ev.getSubject())) {
                if (evidenceProfile.getPan() == null) {
                    evidenceProfile.setPan(val);
                }
            } else if (ev.getSourceType() == SourceType.GST || "GSTIN".equalsIgnoreCase(ev.getAttribute()) || "GSTIN".equalsIgnoreCase(ev.getSubject())) {
                if (evidenceProfile.getGstin() == null) {
                    evidenceProfile.setGstin(val);
                }
            } else if (ev.getSourceType() == SourceType.UDYAM || "UDYAM_NUMBER".equalsIgnoreCase(ev.getAttribute()) || "UDYAM".equalsIgnoreCase(ev.getSubject())) {
                if (evidenceProfile.getUdyamNumber() == null) {
                    evidenceProfile.setUdyamNumber(val);
                }
            } else if ("LEGAL_NAME".equalsIgnoreCase(ev.getAttribute()) || "LEGAL_NAME".equalsIgnoreCase(ev.getSubject())) {
                if (evidenceProfile.getLegalName() == null) {
                    evidenceProfile.setLegalName(val);
                }
            } else if ("ADDRESS".equalsIgnoreCase(ev.getAttribute()) || "REGISTERED_ADDRESS".equalsIgnoreCase(ev.getAttribute())) {
                if (evidenceProfile.getRegisteredAddress() == null) {
                    evidenceProfile.setRegisteredAddress(val);
                }
            }
        }

        EntityResolutionResultDto resultDto = resolve(bidderProfile, evidenceProfile);

        EntityResolutionResult auditResult = EntityResolutionResult.builder()
                .bidder(bidder)
                .comparedSource("VERIFIED_EVIDENCE_AGGREGATE")
                .matchStatus(resultDto.getMatchStatus())
                .confidence(resultDto.getConfidence())
                .matchedAttributes(String.join(",", resultDto.getMatchedAttributes()))
                .mismatchedAttributes(String.join(",", resultDto.getMismatchedAttributes()))
                .missingAttributes(String.join(",", resultDto.getMissingAttributes()))
                .explanation(resultDto.getExplanation())
                .build();
        entityResolutionResultRepository.save(auditResult);

        if (!verifiedEvidences.isEmpty() && resultDto.getMatchStatus() == EntityMatchStatus.MISMATCH) {
            for (Evidence ev : verifiedEvidences) {
                if (resultDto.getMismatchedAttributes().contains(ev.getAttribute())) {
                    EvidenceRelationship rel = EvidenceRelationship.builder()
                            .sourceEvidence(ev)
                            .targetEvidence(ev)
                            .relationshipType(RelationshipType.CONTRADICTS)
                            .reason("Entity resolution detected identity contradiction on " + ev.getAttribute())
                            .confidence(1.0)
                            .build();
                    evidenceRelationshipRepository.save(rel);
                }
            }
        }

        return resultDto;
    }

    private void compareStrongIdentifier(
            String attributeName,
            String leftVal,
            String rightVal,
            List<String> comparedAttributes,
            List<String> matchedAttributes,
            List<String> mismatchedAttributes,
            List<String> missingAttributes,
            List<String> explanations) {

        boolean hasLeft = leftVal != null && !leftVal.isBlank();
        boolean hasRight = rightVal != null && !rightVal.isBlank();

        if (hasLeft && hasRight) {
            comparedAttributes.add(attributeName);
            if (leftVal.equalsIgnoreCase(rightVal)) {
                matchedAttributes.add(attributeName);
                explanations.add(String.format("%s matches exactly ('%s').", attributeName, leftVal));
            } else {
                mismatchedAttributes.add(attributeName);
                explanations.add(String.format("CRITICAL: %s conflicts (Left='%s', Right='%s').", attributeName, leftVal, rightVal));
            }
        } else if (hasLeft || hasRight) {
            missingAttributes.add(attributeName);
        }
    }

    private void checkEmbeddedPanCrossContradiction(
            EntityProfileDto left,
            EntityProfileDto right,
            List<String> comparedAttributes,
            List<String> mismatchedAttributes,
            List<String> explanations) {

        if (left.getPan() != null && !left.getPan().isBlank()
                && right.getGstin() != null && right.getGstin().length() >= 12) {
            String embeddedPanInRightGst = right.getGstin().substring(2, 12);
            if (!comparedAttributes.contains("PAN")) {
                comparedAttributes.add("PAN");
            }
            if (!left.getPan().equalsIgnoreCase(embeddedPanInRightGst)) {
                if (!mismatchedAttributes.contains("PAN")) {
                    mismatchedAttributes.add("PAN");
                    explanations.add(String.format("CRITICAL: Left PAN ('%s') contradicts PAN embedded in Right GSTIN ('%s').",
                            left.getPan(), embeddedPanInRightGst));
                }
            }
        }

        if (right.getPan() != null && !right.getPan().isBlank()
                && left.getGstin() != null && left.getGstin().length() >= 12) {
            String embeddedPanInLeftGst = left.getGstin().substring(2, 12);
            if (!comparedAttributes.contains("PAN")) {
                comparedAttributes.add("PAN");
            }
            if (!right.getPan().equalsIgnoreCase(embeddedPanInLeftGst)) {
                if (!mismatchedAttributes.contains("PAN")) {
                    mismatchedAttributes.add("PAN");
                    explanations.add(String.format("CRITICAL: Right PAN ('%s') contradicts PAN embedded in Left GSTIN ('%s').",
                            right.getPan(), embeddedPanInLeftGst));
                }
            }
        }
    }

    private void compareLegalName(
            String leftName,
            String rightName,
            List<String> comparedAttributes,
            List<String> matchedAttributes,
            List<String> mismatchedAttributes,
            List<String> missingAttributes,
            List<String> explanations) {

        boolean hasLeft = leftName != null && !leftName.isBlank();
        boolean hasRight = rightName != null && !rightName.isBlank();

        if (hasLeft && hasRight) {
            comparedAttributes.add("LEGAL_NAME");
            if (leftName.equalsIgnoreCase(rightName)) {
                matchedAttributes.add("LEGAL_NAME");
                explanations.add(String.format("Normalized legal name matches ('%s').", leftName));
            } else {

                Set<String> leftTokens = new HashSet<>(Arrays.asList(leftName.split("\\s+")));
                Set<String> rightTokens = new HashSet<>(Arrays.asList(rightName.split("\\s+")));

                if (leftTokens.equals(rightTokens)) {
                    matchedAttributes.add("LEGAL_NAME");
                    explanations.add(String.format("Legal name tokens match with word order difference ('%s' vs '%s').", leftName, rightName));
                } else {

                    Set<String> intersection = new HashSet<>(leftTokens);
                    intersection.retainAll(rightTokens);

                    Set<String> union = new HashSet<>(leftTokens);
                    union.addAll(rightTokens);

                    double jaccard = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();

                    if (jaccard >= 0.75) {
                        matchedAttributes.add("LEGAL_NAME");
                        explanations.add(String.format("Legal name shows high deterministic consistency (Jaccard=%.2f).", jaccard));
                    } else {
                        mismatchedAttributes.add("LEGAL_NAME");
                        explanations.add(String.format("Legal name difference (Left='%s', Right='%s').", leftName, rightName));
                    }
                }
            }
        } else if (hasLeft || hasRight) {
            missingAttributes.add("LEGAL_NAME");
        }
    }

    private void compareAddress(
            String leftAddress,
            String rightAddress,
            List<String> comparedAttributes,
            List<String> matchedAttributes,
            List<String> mismatchedAttributes,
            List<String> missingAttributes,
            List<String> explanations) {

        boolean hasLeft = leftAddress != null && !leftAddress.isBlank();
        boolean hasRight = rightAddress != null && !rightAddress.isBlank();

        if (hasLeft && hasRight) {
            comparedAttributes.add("REGISTERED_ADDRESS");
            if (leftAddress.equalsIgnoreCase(rightAddress)) {
                matchedAttributes.add("REGISTERED_ADDRESS");
                explanations.add(String.format("Normalized address matches ('%s').", leftAddress));
            } else {
                Set<String> leftTokens = new HashSet<>(Arrays.asList(leftAddress.split("\\s+")));
                Set<String> rightTokens = new HashSet<>(Arrays.asList(rightAddress.split("\\s+")));

                Set<String> intersection = new HashSet<>(leftTokens);
                intersection.retainAll(rightTokens);

                Set<String> union = new HashSet<>(leftTokens);
                union.addAll(rightTokens);

                double jaccard = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();

                boolean isSubsetMatch = (leftTokens.containsAll(rightTokens) || rightTokens.containsAll(leftTokens)) && intersection.size() >= 3;

                if (jaccard >= 0.65 || isSubsetMatch) {
                    matchedAttributes.add("REGISTERED_ADDRESS");
                    explanations.add(String.format("Normalized address tokens consistent with minor variation (Overlap=%d/%d).",
                            intersection.size(), union.size()));
                } else if (jaccard <= 0.25) {
                    mismatchedAttributes.add("REGISTERED_ADDRESS");
                    explanations.add(String.format("Registered address major discrepancy (Left='%s', Right='%s').", leftAddress, rightAddress));
                } else {

                    explanations.add(String.format("Registered address partial variation (Overlap=%d/%d).", intersection.size(), union.size()));
                }
            }
        } else if (hasLeft || hasRight) {
            missingAttributes.add("REGISTERED_ADDRESS");
        }
    }

    private void compareContact(
            String attributeName,
            String leftContact,
            String rightContact,
            List<String> comparedAttributes,
            List<String> matchedAttributes,
            List<String> mismatchedAttributes,
            List<String> missingAttributes,
            List<String> explanations) {

        boolean hasLeft = leftContact != null && !leftContact.isBlank();
        boolean hasRight = rightContact != null && !rightContact.isBlank();

        if (hasLeft && hasRight) {
            comparedAttributes.add(attributeName);
            if (leftContact.equalsIgnoreCase(rightContact)) {
                matchedAttributes.add(attributeName);
                explanations.add(String.format("%s matches ('%s').", attributeName, leftContact));
            } else {

                mismatchedAttributes.add(attributeName);
                explanations.add(String.format("Different %s contact noted (Left='%s', Right='%s').", attributeName, leftContact, rightContact));
            }
        } else if (hasLeft || hasRight) {
            missingAttributes.add(attributeName);
        }
    }

    private EntityResolutionResultDto evaluateDecision(
            List<String> comparedAttributes,
            List<String> matchedAttributes,
            List<String> mismatchedAttributes,
            List<String> missingAttributes,
            List<String> explanations) {

        boolean hasStrongMismatches = mismatchedAttributes.stream()
                .anyMatch(attr -> "PAN".equals(attr) || "GSTIN".equals(attr) || "UDYAM_NUMBER".equals(attr));

        boolean hasStrongMatches = matchedAttributes.stream()
                .anyMatch(attr -> "PAN".equals(attr) || "GSTIN".equals(attr) || "UDYAM_NUMBER".equals(attr));

        boolean hasNameMatch = matchedAttributes.contains("LEGAL_NAME");
        boolean hasNameMismatch = mismatchedAttributes.contains("LEGAL_NAME");
        boolean hasAddressMatch = matchedAttributes.contains("REGISTERED_ADDRESS");
        boolean hasAddressMismatch = mismatchedAttributes.contains("REGISTERED_ADDRESS");

        if (hasStrongMismatches) {
            List<String> conflicting = mismatchedAttributes.stream()
                    .filter(attr -> "PAN".equals(attr) || "GSTIN".equals(attr) || "UDYAM_NUMBER".equals(attr))
                    .collect(Collectors.toList());

            return EntityResolutionResultDto.builder()
                    .matchStatus(EntityMatchStatus.MISMATCH)
                    .confidence(0.0)
                    .comparedAttributes(comparedAttributes)
                    .matchedAttributes(matchedAttributes)
                    .mismatchedAttributes(mismatchedAttributes)
                    .missingAttributes(missingAttributes)
                    .explanation("MISMATCH: Hard contradiction detected in statutory identifier(s): " + String.join(", ", conflicting)
                            + ". " + String.join(" ", explanations))
                    .build();
        }

        long significantComparedCount = comparedAttributes.stream()
                .filter(attr -> !"EMAIL".equals(attr) && !"PHONE".equals(attr))
                .count();

        if (significantComparedCount == 0) {
            return EntityResolutionResultDto.builder()
                    .matchStatus(EntityMatchStatus.INSUFFICIENT_DATA)
                    .confidence(0.0)
                    .comparedAttributes(comparedAttributes)
                    .matchedAttributes(matchedAttributes)
                    .mismatchedAttributes(mismatchedAttributes)
                    .missingAttributes(missingAttributes)
                    .explanation("INSUFFICIENT_DATA: No core identity attributes (Name, Address, PAN, GSTIN, Udyam) available to compare.")
                    .build();
        }

        if (!hasStrongMatches && hasNameMismatch && hasAddressMismatch) {
            return EntityResolutionResultDto.builder()
                    .matchStatus(EntityMatchStatus.MISMATCH)
                    .confidence(0.0)
                    .comparedAttributes(comparedAttributes)
                    .matchedAttributes(matchedAttributes)
                    .mismatchedAttributes(mismatchedAttributes)
                    .missingAttributes(missingAttributes)
                    .explanation("MISMATCH: Multiple independent identity attributes (Legal Name and Address) contradict without strong identifier bridge. "
                            + String.join(" ", explanations))
                    .build();
        }

        if (hasStrongMatches) {
            double confidence = 1.0;

            String summary;
            if (hasNameMatch) {
                summary = "MATCH: Statutory identifier(s) match and normalized legal name is consistent.";
            } else {
                summary = "MATCH: Verified statutory identifier(s) match with high identity strength.";
            }

            return EntityResolutionResultDto.builder()
                    .matchStatus(EntityMatchStatus.MATCH)
                    .confidence(confidence)
                    .comparedAttributes(comparedAttributes)
                    .matchedAttributes(matchedAttributes)
                    .mismatchedAttributes(mismatchedAttributes)
                    .missingAttributes(missingAttributes)
                    .explanation(summary + " " + String.join(" ", explanations))
                    .build();
        }

        if (hasNameMatch && hasAddressMatch) {
            return EntityResolutionResultDto.builder()
                    .matchStatus(EntityMatchStatus.PROBABLE_MATCH)
                    .confidence(0.80)
                    .comparedAttributes(comparedAttributes)
                    .matchedAttributes(matchedAttributes)
                    .mismatchedAttributes(mismatchedAttributes)
                    .missingAttributes(missingAttributes)
                    .explanation("PROBABLE_MATCH: Legal name and registered address match, but statutory strong identifiers (PAN/GSTIN) are unavailable. "
                            + String.join(" ", explanations))
                    .build();
        }

        if (hasNameMatch) {
            return EntityResolutionResultDto.builder()
                    .matchStatus(EntityMatchStatus.PROBABLE_MATCH)
                    .confidence(0.65)
                    .comparedAttributes(comparedAttributes)
                    .matchedAttributes(matchedAttributes)
                    .mismatchedAttributes(mismatchedAttributes)
                    .missingAttributes(missingAttributes)
                    .explanation("PROBABLE_MATCH: Normalized legal name matches, but primary statutory identifiers are missing. "
                            + String.join(" ", explanations))
                    .build();
        }

        return EntityResolutionResultDto.builder()
                .matchStatus(EntityMatchStatus.INSUFFICIENT_DATA)
                .confidence(0.0)
                .comparedAttributes(comparedAttributes)
                .matchedAttributes(matchedAttributes)
                .mismatchedAttributes(mismatchedAttributes)
                .missingAttributes(missingAttributes)
                .explanation("INSUFFICIENT_DATA: Available identity information is inconclusive. " + String.join(" ", explanations))
                .build();
    }

}
