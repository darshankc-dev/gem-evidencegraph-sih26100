package com.gem.evidencegraph.dto;

import com.gem.evidencegraph.entity.EvidenceType;
import com.gem.evidencegraph.entity.RelationshipType;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceGraphResponseDto {

    private UUID id;
    private UUID bidId;
    private UUID claimId;
    private EvidenceType evidenceType;
    private SourceType sourceType;
    private String sourceReference;
    private String sourceSystem;
    private String adapterVersion;
    private String subject;
    private String attribute;
    private String value;
    private String normalizedValue;
    private VerificationStatus verificationStatus;
    private LocalDateTime observedAt;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime retrievedAt;
    private Double confidence;
    private String provenanceHash;

    @Builder.Default
    private List<RelationshipItemDto> relationships = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationshipItemDto {
        private UUID relationshipId;
        private UUID sourceClaimId;
        private UUID sourceEvidenceId;
        private UUID targetEvidenceId;
        private RelationshipType relationshipType;
        private String reason;
        private Double confidence;
    }

}
