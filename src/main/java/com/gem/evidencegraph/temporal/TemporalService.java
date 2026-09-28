package com.gem.evidencegraph.temporal;

import com.gem.evidencegraph.temporal.dto.BidTemporalEvaluationResultDto;
import com.gem.evidencegraph.temporal.dto.TemporalEvaluationResponseDto;

import java.util.List;
import java.util.UUID;

public interface TemporalService {

    BidTemporalEvaluationResultDto evaluateBidTemporal(UUID bidId);

    BidTemporalEvaluationResultDto getBidTemporal(UUID bidId);

    List<TemporalEvaluationResponseDto> getEvidenceTemporal(UUID evidenceId);

}
