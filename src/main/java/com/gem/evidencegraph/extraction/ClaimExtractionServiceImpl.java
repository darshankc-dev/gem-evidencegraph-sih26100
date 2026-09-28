package com.gem.evidencegraph.extraction;

import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.ExtractionMethod;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ClaimExtractionServiceImpl implements ClaimExtractionService {

    private static final double CONFIDENCE_UNAMBIGUOUS = 0.95;
    private static final double CONFIDENCE_AMBIGUOUS = 0.75;

    @Override
    public List<Claim> extractClaims(Document document, DocumentTextResult textResult) {
        if (textResult == null || !textResult.isHasTextLayer() || textResult.getFullText().isBlank()) {
            return Collections.emptyList();
        }

        List<RawClaimCandidate> rawCandidates = new ArrayList<>();
        Map<Integer, String> pageTexts = textResult.getPageTexts();

        if (pageTexts != null && !pageTexts.isEmpty()) {
            for (Map.Entry<Integer, String> entry : pageTexts.entrySet()) {
                int pageNumber = entry.getKey();
                String pageText = entry.getValue();
                scanTextForCandidates(pageText, pageNumber, rawCandidates);
            }
        } else {
            scanTextForCandidates(textResult.getFullText(), 1, rawCandidates);
        }

        List<RawClaimCandidate> uniqueCandidates = deduplicateCandidates(rawCandidates);

        Map<String, Set<String>> fieldValuesMap = new LinkedHashMap<>();
        for (RawClaimCandidate candidate : uniqueCandidates) {
            fieldValuesMap.computeIfAbsent(candidate.fieldName(), k -> new HashSet<>())
                    .add(candidate.normalizedValue());
        }

        List<Claim> claims = new ArrayList<>();
        for (RawClaimCandidate candidate : uniqueCandidates) {
            Set<String> distinctValues = fieldValuesMap.getOrDefault(candidate.fieldName(), Collections.emptySet());
            double confidence = distinctValues.size() > 1 ? CONFIDENCE_AMBIGUOUS : CONFIDENCE_UNAMBIGUOUS;

            Claim claim = Claim.builder()
                    .id(UUID.randomUUID())
                    .document(document)
                    .fieldName(candidate.fieldName())
                    .fieldValue(candidate.rawFieldValue())
                    .normalizedValue(candidate.normalizedValue())
                    .confidence(confidence)
                    .extractionMethod(ExtractionMethod.DETERMINISTIC)
                    .sourcePage(candidate.pageNumber())
                    .sourceLocation(candidate.sourceLocation())
                    .build();

            claims.add(claim);
        }

        return claims;
    }

    private void scanTextForCandidates(String text, int pageNumber, List<RawClaimCandidate> candidates) {

        matchPattern(text, ClaimPatternRegistry.PAN_PATTERN, "PAN", "Detected via PAN format pattern", pageNumber, candidates);

        matchPattern(text, ClaimPatternRegistry.GSTIN_PATTERN, "GSTIN", "Detected via GSTIN format pattern", pageNumber, candidates);

        matchPattern(text, ClaimPatternRegistry.UDYAM_PATTERN, "UDYAM_NUMBER", "Detected via Udyam format pattern", pageNumber, candidates);

        matchPattern(text, ClaimPatternRegistry.LEGAL_NAME_LABEL, "LEGAL_NAME", "Detected near label: Legal Name", pageNumber, candidates);

        matchPattern(text, ClaimPatternRegistry.ADDRESS_LABEL, "ADDRESS", "Detected near label: Address", pageNumber, candidates);

        matchPattern(text, ClaimPatternRegistry.REGISTRATION_DATE_LABEL, "REGISTRATION_DATE", "Detected near label: Registration Date", pageNumber, candidates);

        matchPattern(text, ClaimPatternRegistry.EXPIRY_DATE_LABEL, "EXPIRY_DATE", "Detected near label: Expiry Date", pageNumber, candidates);

        matchPattern(text, ClaimPatternRegistry.CERTIFICATE_NUMBER_LABEL, "CERTIFICATE_NUMBER", "Detected near label: Certificate Number", pageNumber, candidates);
    }

    private void matchPattern(String text, Pattern pattern, String fieldName, String locationDesc, int pageNumber, List<RawClaimCandidate> candidates) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String rawValue = matcher.group(1).trim();
            if (rawValue.isBlank()) {
                continue;
            }
            String normalizedValue = ClaimNormalizationUtil.normalize(fieldName, rawValue);
            if (!normalizedValue.isBlank()) {
                candidates.add(new RawClaimCandidate(
                        fieldName,
                        rawValue,
                        normalizedValue,
                        pageNumber,
                        locationDesc + " on page " + pageNumber
                ));
            }
        }
    }

    private List<RawClaimCandidate> deduplicateCandidates(List<RawClaimCandidate> candidates) {
        Set<String> seen = new HashSet<>();
        List<RawClaimCandidate> result = new ArrayList<>();
        for (RawClaimCandidate c : candidates) {
            String key = c.fieldName() + "::" + c.normalizedValue() + "::" + c.pageNumber();
            if (seen.add(key)) {
                result.add(c);
            }
        }
        return result;
    }

    private record RawClaimCandidate(
            String fieldName,
            String rawFieldValue,
            String normalizedValue,
            int pageNumber,
            String sourceLocation
    ) {
    }

}
