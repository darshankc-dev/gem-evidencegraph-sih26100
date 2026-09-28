package com.gem.evidencegraph.temporal.dto;

import com.gem.evidencegraph.temporal.TemporalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporalEvaluationResponseDto {

    private UUID evaluationId;
    private UUID bidId;
    private UUID tenderId;
    private UUID evidenceId;
    private LocalDateTime referenceDate;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime observedAt;
    private LocalDateTime issueDate;
    private TemporalStatus status;
    private String reason;
    private LocalDateTime evaluatedAt;

}
