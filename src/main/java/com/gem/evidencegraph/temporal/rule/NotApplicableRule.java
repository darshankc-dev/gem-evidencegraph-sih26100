package com.gem.evidencegraph.temporal.rule;

import com.gem.evidencegraph.temporal.TemporalEvaluationContext;
import com.gem.evidencegraph.temporal.TemporalStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(80)
public class NotApplicableRule implements TemporalRule {

    public static final String RULE_NAME = "NotApplicableRule";
    public static final String REASON = "Evidence does not require temporal evaluation.";

    @Override
    public boolean supports(TemporalEvaluationContext context) {
        return context != null && !context.isTemporalEvaluationRequired();
    }

    @Override
    public TemporalRuleResult evaluate(TemporalEvaluationContext context) {
        if (!context.isTemporalEvaluationRequired()) {
            return TemporalRuleResult.of(TemporalStatus.NOT_APPLICABLE, REASON, RULE_NAME, getPriority());
        }
        return TemporalRuleResult.notMatched();
    }

    @Override
    public int getPriority() {
        return 8;
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

}
