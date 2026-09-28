package com.gem.evidencegraph.risk.evaluator;

import com.gem.evidencegraph.entity.RiskFinding;
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
public class HardFailResult {

    private boolean hardFail;

    @Builder.Default
    private List<String> reasons = new ArrayList<>();

    @Builder.Default
    private List<RiskFinding> hardFailFindings = new ArrayList<>();

}
