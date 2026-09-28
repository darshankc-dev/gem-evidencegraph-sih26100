package com.gem.evidencegraph.dto;

import com.gem.evidencegraph.entity.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequestDto {

    private UUID claimId;
    private UUID bidId;
    private SourceType requestedSourceType;
    private String attribute;

}
