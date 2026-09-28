package com.gem.evidencegraph.risk.dto;

import com.gem.evidencegraph.risk.RiskDimension;
import com.gem.evidencegraph.risk.RiskSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskFindingDto {

    private RiskDimension dimension;
    private RiskSeverity severity;
    private String code;
    private String title;
    private String description;
    private String sourceType;
    private String sourceId;
    private String rule;
    private int weight;

}
