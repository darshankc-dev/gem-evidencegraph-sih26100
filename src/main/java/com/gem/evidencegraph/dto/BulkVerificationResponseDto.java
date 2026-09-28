package com.gem.evidencegraph.dto;

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
public class BulkVerificationResponseDto {

    private UUID bidId;
    private int totalClaimsScanned;
    private int verifiedCount;
    private int mismatchCount;
    private int unverifiedCount;
    private int unavailableCount;
    private List<VerificationResponseDto> results;

}
