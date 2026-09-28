package com.gem.evidencegraph.extraction;

import com.gem.evidencegraph.dto.ClaimResponseDto;
import com.gem.evidencegraph.dto.ExtractionResponseDto;

import java.util.List;
import java.util.UUID;

public interface DocumentExtractionPipelineService {

    ExtractionResponseDto executeExtraction(UUID documentId);

    List<ClaimResponseDto> getClaimsByDocument(UUID documentId);

}
