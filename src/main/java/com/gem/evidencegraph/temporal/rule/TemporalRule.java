package com.gem.evidencegraph.temporal.rule;

import com.gem.evidencegraph.temporal.TemporalEvaluationContext;

public interface TemporalRule {

    boolean supports(TemporalEvaluationContext context);

    TemporalRuleResult evaluate(TemporalEvaluationContext context);

    int getPriority();

    String getName();

}
