package com.gem.evidencegraph.temporal.rule;

import com.gem.evidencegraph.temporal.TemporalEvaluationContext;
import com.gem.evidencegraph.temporal.TemporalStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class MissingTemporalDataRule implements TemporalRule {

    public static final String RULE_NAME = "MissingTemporalDataRule";
    public static final String REASON = "Required temporal information is unavailable.";

    @Override
    public boolean supports(TemporalEvaluationContext context) {
        return context != null && context.isTemporalEvaluationRequired();
    }

    @Override
    public TemporalRuleResult evaluate(TemporalEvaluationContext context) {

        if (context.getBidSubmissionDate() == null) {
            return TemporalRuleResult.of(TemporalStatus.MISSING_TEMPORAL_DATA, REASON, RULE_NAME, getPriority());
        }

        boolean hasDates = context.getValidFrom() != null
                || context.getValidUntil() != null
                || context.getIssueDate() != null;

        if (!hasDates) {
            return TemporalRuleResult.of(TemporalStatus.MISSING_TEMPORAL_DATA, REASON, RULE_NAME, getPriority());
        }

        return TemporalRuleResult.notMatched();
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

}
