package com.gem.evidencegraph.verification;

import com.gem.evidencegraph.dto.VerificationResultDto;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.SourceType;

public interface VerificationAdapter {

    SourceType getSourceType();

    String getAdapterVersion();

    boolean supports(Claim claim);

    VerificationResultDto verify(Claim claim);

}
