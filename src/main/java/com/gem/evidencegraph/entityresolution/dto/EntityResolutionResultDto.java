package com.gem.evidencegraph.entityresolution.dto;

import com.gem.evidencegraph.entityresolution.EntityMatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityResolutionResultDto {

    private EntityMatchStatus matchStatus;
    private Double confidence;

    @Builder.Default
    private List<String> comparedAttributes = new ArrayList<>();

    @Builder.Default
    private List<String> matchedAttributes = new ArrayList<>();

    @Builder.Default
    private List<String> mismatchedAttributes = new ArrayList<>();

    @Builder.Default
    private List<String> missingAttributes = new ArrayList<>();

    private String explanation;

}
