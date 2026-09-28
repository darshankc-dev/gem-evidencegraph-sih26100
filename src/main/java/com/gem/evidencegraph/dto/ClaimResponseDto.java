package com.gem.evidencegraph.dto;

import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.ExtractionMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponseDto {

    private UUID id;
    private UUID documentId;
    private String fieldName;
    private String fieldValue;
    private String normalizedValue;
    private Double confidence;
    private ExtractionMethod extractionMethod;
    private Integer sourcePage;
    private String sourceLocation;
    private LocalDateTime createdAt;

    public static ClaimResponseDto fromEntity(Claim claim) {
        if (claim == null) {
            return null;
        }
        return ClaimResponseDto.builder()
                .id(claim.getId())
                .documentId(claim.getDocument() != null ? claim.getDocument().getId() : null)
                .fieldName(claim.getFieldName())
                .fieldValue(claim.getFieldValue())
                .normalizedValue(claim.getNormalizedValue())
                .confidence(claim.getConfidence())
                .extractionMethod(claim.getExtractionMethod())
                .sourcePage(claim.getSourcePage())
                .sourceLocation(claim.getSourceLocation())
                .createdAt(claim.getCreatedAt())
                .build();
    }

}
