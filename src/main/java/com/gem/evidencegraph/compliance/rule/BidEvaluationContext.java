package com.gem.evidencegraph.compliance.rule;

import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Bidder;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.Evidence;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.Tender;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Builder
public class BidEvaluationContext {

    private final Bid bid;
    private final Bidder bidder;
    private final Tender tender;
    private final List<Document> documents;
    private final List<Claim> claims;
    private final List<Evidence> evidences;

    public List<Document> getDocumentsByType(DocumentType type) {
        if (documents == null || type == null) {
            return Collections.emptyList();
        }
        return documents.stream()
                .filter(d -> type.equals(d.getDocumentType()))
                .collect(Collectors.toList());
    }

    public List<Claim> getClaimsByField(String fieldName) {
        if (claims == null || fieldName == null) {
            return Collections.emptyList();
        }
        return claims.stream()
                .filter(c -> fieldName.equalsIgnoreCase(c.getFieldName()))
                .collect(Collectors.toList());
    }

    public List<Evidence> getEvidencesBySource(SourceType sourceType) {
        if (evidences == null || sourceType == null) {
            return Collections.emptyList();
        }
        return evidences.stream()
                .filter(e -> sourceType.equals(e.getSourceType()))
                .collect(Collectors.toList());
    }

    public List<Evidence> getEvidencesByAttribute(String attribute) {
        if (evidences == null || attribute == null) {
            return Collections.emptyList();
        }
        return evidences.stream()
                .filter(e -> attribute.equalsIgnoreCase(e.getAttribute()) || attribute.equalsIgnoreCase(e.getSubject()))
                .collect(Collectors.toList());
    }

}
