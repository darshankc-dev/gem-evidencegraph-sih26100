package com.gem.evidencegraph.risk;

import com.gem.evidencegraph.risk.dto.BidRiskAssessmentResponseDto;
import com.gem.evidencegraph.risk.dto.RiskDimensionResultDto;
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
public class RiskController {

    private final RiskService riskService;

    @PostMapping("/bids/{bidId}/risk/evaluate")
    public ResponseEntity<BidRiskAssessmentResponseDto> evaluateBidRisk(
            @PathVariable UUID bidId) {
        log.info("Received request to evaluate risk for bid ID: {}", bidId);
        BidRiskAssessmentResponseDto response = riskService.evaluateBidRisk(bidId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bids/{bidId}/risk")
    public ResponseEntity<BidRiskAssessmentResponseDto> getBidRisk(
            @PathVariable UUID bidId) {
        log.info("Received request to retrieve risk assessment for bid ID: {}", bidId);
        BidRiskAssessmentResponseDto response = riskService.getBidRisk(bidId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bids/{bidId}/risk/dimensions")
    public ResponseEntity<List<RiskDimensionResultDto>> getBidRiskDimensions(
            @PathVariable UUID bidId) {
        log.info("Received request to retrieve risk dimensions for bid ID: {}", bidId);
        List<RiskDimensionResultDto> response = riskService.getBidRiskDimensions(bidId);
        return ResponseEntity.ok(response);
    }

}
