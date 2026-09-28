package com.gem.evidencegraph.temporal.rule;

import com.gem.evidencegraph.temporal.TemporalEvaluationContext;
import com.gem.evidencegraph.temporal.TemporalStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(10)
public class DateConsistencyRule implements TemporalRule {

    public static final String RULE_NAME = "DateConsistencyRule";
    public static final String REASON = "Temporal fields contain conflicting values.";

    @Override
    public boolean supports(TemporalEvaluationContext context) {
        return context != null && context.isTemporalEvaluationRequired();
    }

    @Override
    public TemporalRuleResult evaluate(TemporalEvaluationContext context) {
        if (context.isConflictingDatesDetected()) {
            return TemporalRuleResult.of(TemporalStatus.CONFLICTING_DATES, REASON, RULE_NAME, getPriority());
        }

        LocalDateTime validFrom = context.getValidFrom();
        LocalDateTime validUntil = context.getValidUntil();
        LocalDateTime issueDate = context.getIssueDate();

        if (validFrom != null && validUntil != null && validFrom.isAfter(validUntil)) {
            return TemporalRuleResult.of(TemporalStatus.CONFLICTING_DATES, REASON, RULE_NAME, getPriority());
        }

        if (issueDate != null && validUntil != null && issueDate.isAfter(validUntil)) {
            return TemporalRuleResult.of(TemporalStatus.CONFLICTING_DATES, REASON, RULE_NAME, getPriority());
        }

        return TemporalRuleResult.notMatched();
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

}
