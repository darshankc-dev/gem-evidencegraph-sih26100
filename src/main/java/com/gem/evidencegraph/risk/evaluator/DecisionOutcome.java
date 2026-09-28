package com.gem.evidencegraph.risk.evaluator;

import com.gem.evidencegraph.risk.DecisionRecommendation;
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
public class DecisionOutcome {

    private DecisionRecommendation recommendation;
    private String reason;
    private String summary;

    @Builder.Default
    private List<String> reviewTriggers = new ArrayList<>();

}
