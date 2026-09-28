package com.gem.evidencegraph.compliance;

import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.compliance.dto.CreateRequirementRequestDto;
import com.gem.evidencegraph.compliance.dto.RequirementResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceService complianceService;

    @PostMapping("/tenders/{tenderId}/requirements")
    public ResponseEntity<RequirementResponseDto> createRequirement(
            @PathVariable UUID tenderId,
            @Valid @RequestBody CreateRequirementRequestDto request) {
        log.info("Creating compliance requirement '{}' for tender ID: {}", request.getRequirementCode(), tenderId);
        RequirementResponseDto response = complianceService.createRequirement(tenderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/tenders/{tenderId}/requirements")
    public ResponseEntity<List<RequirementResponseDto>> getRequirementsByTender(
            @PathVariable UUID tenderId) {
        log.info("Retrieving compliance requirements for tender ID: {}", tenderId);
        List<RequirementResponseDto> response = complianceService.getRequirementsByTender(tenderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/requirements/{requirementId}")
    public ResponseEntity<RequirementResponseDto> getRequirementById(
            @PathVariable UUID requirementId) {
        log.info("Retrieving requirement with ID: {}", requirementId);
        RequirementResponseDto response = complianceService.getRequirementById(requirementId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bids/{bidId}/compliance/evaluate")
    public ResponseEntity<BidComplianceResultDto> evaluateBidCompliance(
            @PathVariable UUID bidId) {
        log.info("Evaluating tender compliance for bid ID: {}", bidId);
        BidComplianceResultDto response = complianceService.evaluateBidCompliance(bidId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bids/{bidId}/compliance")
    public ResponseEntity<BidComplianceResultDto> getLatestBidCompliance(
            @PathVariable UUID bidId) {
        log.info("Retrieving latest compliance evaluation for bid ID: {}", bidId);
        BidComplianceResultDto response = complianceService.getLatestBidCompliance(bidId);
        return ResponseEntity.ok(response);
    }

}
