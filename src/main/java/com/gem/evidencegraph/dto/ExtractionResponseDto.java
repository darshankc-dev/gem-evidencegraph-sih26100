package com.gem.evidencegraph.dto;

import com.gem.evidencegraph.entity.ExtractionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResponseDto {

    private UUID documentId;
    private ExtractionStatus extractionStatus;
    private int extractedTextLength;
    private int numberOfClaims;
    private List<ClaimResponseDto> claims;

}
