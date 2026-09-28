package com.gem.evidencegraph.risk.dto;

import com.gem.evidencegraph.risk.RiskDimension;
import com.gem.evidencegraph.risk.RiskLevel;
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
public class RiskDimensionResultDto {

    private RiskDimension dimension;
    private RiskLevel riskLevel;
    private int riskScore;
    private int findingCount;
    private String summary;

    @Builder.Default
    private List<RiskFindingDto> findings = new ArrayList<>();

}
