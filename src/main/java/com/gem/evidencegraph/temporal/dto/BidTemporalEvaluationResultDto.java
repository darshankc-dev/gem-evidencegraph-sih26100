package com.gem.evidencegraph.temporal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidTemporalEvaluationResultDto {

    private UUID bidId;
    private UUID tenderId;
    private LocalDateTime evaluatedAt;
    private int totalEvaluated;
    private int validCount;
    private int expiredCount;
    private int futureValidityCount;
    private int issuedAfterBidCount;
    private int issuedAfterTenderPublicationCount;
    private int conflictingCount;
    private int missingTemporalDataCount;
    private int notApplicableCount;

    @Builder.Default
    private List<TemporalEvaluationResponseDto> results = new ArrayList<>();

}
