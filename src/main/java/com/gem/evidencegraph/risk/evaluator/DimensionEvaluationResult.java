package com.gem.evidencegraph.risk.evaluator;

import com.gem.evidencegraph.entity.RiskFinding;
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
public class DimensionEvaluationResult {

    private RiskDimension dimension;
    private int riskScore;
    private RiskLevel riskLevel;
    private int findingCount;
    private String summary;
    private String reason;

    @Builder.Default
    private List<RiskFinding> findings = new ArrayList<>();

}
