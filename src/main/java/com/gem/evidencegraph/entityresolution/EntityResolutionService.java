package com.gem.evidencegraph.entityresolution;

import com.gem.evidencegraph.entityresolution.dto.EntityProfileDto;
import com.gem.evidencegraph.entityresolution.dto.EntityResolutionResultDto;

import java.util.UUID;

public interface EntityResolutionService {

    EntityResolutionResultDto resolve(EntityProfileDto left, EntityProfileDto right);

    EntityResolutionResultDto resolveBidderAgainstEvidence(UUID bidderId);

}
