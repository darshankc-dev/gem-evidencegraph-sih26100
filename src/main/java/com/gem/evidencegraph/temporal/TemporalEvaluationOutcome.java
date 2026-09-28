package com.gem.evidencegraph.temporal;

import com.gem.evidencegraph.temporal.rule.TemporalRuleResult;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class TemporalEvaluationOutcome {

    private final TemporalStatus primaryStatus;
    private final String reason;
    private final String ruleApplied;
    @Builder.Default
    private final List<TemporalRuleResult> allFindings = Collections.emptyList();

}
