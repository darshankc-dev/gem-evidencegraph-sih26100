package com.gem.evidencegraph.temporal.rule;

import com.gem.evidencegraph.temporal.TemporalEvaluationContext;
import com.gem.evidencegraph.temporal.TemporalStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(30)
public class FutureValidityRule implements TemporalRule {

    public static final String RULE_NAME = "FutureValidityRule";
    public static final String REASON = "Evidence becomes valid after the bid submission date.";

    @Override
    public boolean supports(TemporalEvaluationContext context) {
        return context != null
                && context.getValidFrom() != null
                && context.getBidSubmissionDate() != null;
    }

    @Override
    public TemporalRuleResult evaluate(TemporalEvaluationContext context) {
        LocalDateTime validFrom = context.getValidFrom();
        LocalDateTime bidDate = context.getBidSubmissionDate();

        if (validFrom.toLocalDate().isAfter(bidDate.toLocalDate())) {
            return TemporalRuleResult.of(TemporalStatus.NOT_YET_VALID_AT_BID_DATE, REASON, RULE_NAME, getPriority());
        }

        return TemporalRuleResult.notMatched();
    }

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

}
