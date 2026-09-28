package com.gem.evidencegraph.temporal.rule;

import com.gem.evidencegraph.temporal.TemporalEvaluationContext;
import com.gem.evidencegraph.temporal.TemporalStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(40)
public class ExpirationRule implements TemporalRule {

    public static final String RULE_NAME = "ExpirationRule";
    public static final String REASON = "Evidence expired before the bid submission date.";

    @Override
    public boolean supports(TemporalEvaluationContext context) {
        return context != null
                && context.getValidUntil() != null
                && context.getBidSubmissionDate() != null;
    }

    @Override
    public TemporalRuleResult evaluate(TemporalEvaluationContext context) {
        LocalDateTime validUntil = context.getValidUntil();
        LocalDateTime bidDate = context.getBidSubmissionDate();

        if (validUntil.toLocalDate().isBefore(bidDate.toLocalDate())) {
            return TemporalRuleResult.of(TemporalStatus.EXPIRED_AT_BID_DATE, REASON, RULE_NAME, getPriority());
        }

        return TemporalRuleResult.notMatched();
    }

    @Override
    public int getPriority() {
        return 4;
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

}
