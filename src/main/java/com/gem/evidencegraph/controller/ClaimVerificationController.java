package com.gem.evidencegraph.controller;

import com.gem.evidencegraph.dto.BulkVerificationResponseDto;
import com.gem.evidencegraph.dto.VerificationRequestDto;
import com.gem.evidencegraph.dto.VerificationResponseDto;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.verification.gateway.VerificationGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClaimVerificationController {

    private final VerificationGateway verificationGateway;

    @PostMapping("/claims/{claimId}/verify")
    public ResponseEntity<VerificationResponseDto> verifyClaim(
            @PathVariable UUID claimId,
            @RequestBody(required = false) VerificationRequestDto request) {

        log.info("Received verification request for claim {}", claimId);
        VerificationRequestDto effectiveRequest = request != null ? request : new VerificationRequestDto();
        effectiveRequest.setClaimId(claimId);

        VerificationResponseDto response = verificationGateway.verifyClaim(effectiveRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bids/{bidId}/verify")
    public ResponseEntity<BulkVerificationResponseDto> verifyBidClaims(
            @PathVariable UUID bidId,
            @RequestParam(required = false) SourceType sourceType) {

        log.info("Received bulk verification request for bid {}", bidId);
        BulkVerificationResponseDto response = verificationGateway.verifyBidClaims(bidId, sourceType);
        return ResponseEntity.ok(response);
    }

}
