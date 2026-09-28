package com.gem.evidencegraph.verification.gateway;

import com.gem.evidencegraph.dto.BulkVerificationResponseDto;
import com.gem.evidencegraph.dto.VerificationRequestDto;
import com.gem.evidencegraph.dto.VerificationResponseDto;
import com.gem.evidencegraph.entity.SourceType;

import java.util.UUID;

public interface VerificationGateway {

    VerificationResponseDto verifyClaim(VerificationRequestDto request);

    BulkVerificationResponseDto verifyBidClaims(UUID bidId, SourceType preferredSourceType);

}
