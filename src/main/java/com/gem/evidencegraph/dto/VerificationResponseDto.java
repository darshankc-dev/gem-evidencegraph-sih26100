package com.gem.evidencegraph.dto;

import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponseDto {

    private UUID claimId;
    private SourceType sourceType;
    private VerificationStatus verificationStatus;
    private String expectedValue;
    private String observedValue;
    private UUID evidenceId;
    private UUID relationshipId;
    private Double confidence;
    private String reason;

}
