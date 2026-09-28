package com.gem.evidencegraph.temporal;

import com.gem.evidencegraph.temporal.dto.BidTemporalEvaluationResultDto;
import com.gem.evidencegraph.temporal.dto.TemporalEvaluationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TemporalVerificationController {

    private final TemporalService temporalService;

    @PostMapping("/bids/{bidId}/temporal/evaluate")
    public ResponseEntity<BidTemporalEvaluationResultDto> evaluateBidTemporal(
            @PathVariable UUID bidId) {
        log.info("Received request to evaluate temporal validity for bid ID: {}", bidId);
        BidTemporalEvaluationResultDto response = temporalService.evaluateBidTemporal(bidId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bids/{bidId}/temporal")
    public ResponseEntity<BidTemporalEvaluationResultDto> getBidTemporal(
            @PathVariable UUID bidId) {
        log.info("Received request to retrieve temporal evaluation for bid ID: {}", bidId);
        BidTemporalEvaluationResultDto response = temporalService.getBidTemporal(bidId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/evidence/{evidenceId}/temporal")
    public ResponseEntity<List<TemporalEvaluationResponseDto>> getEvidenceTemporal(
            @PathVariable UUID evidenceId) {
        log.info("Received request to retrieve temporal evaluation for evidence ID: {}", evidenceId);
        List<TemporalEvaluationResponseDto> response = temporalService.getEvidenceTemporal(evidenceId);
        return ResponseEntity.ok(response);
    }

}
