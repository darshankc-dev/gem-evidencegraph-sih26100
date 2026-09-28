package com.gem.evidencegraph.temporal.rule;

import com.gem.evidencegraph.temporal.TemporalEvaluationContext;
import com.gem.evidencegraph.temporal.TemporalStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(70)
public class ValidityWindowRule implements TemporalRule {

    public static final String RULE_NAME = "ValidityWindowRule";
    public static final String REASON = "Evidence validity window covers the bid submission date.";

    @Override
    public boolean supports(TemporalEvaluationContext context) {
        if (context == null || context.getBidSubmissionDate() == null) {
            return false;
        }
        return context.getValidFrom() != null
                || context.getValidUntil() != null
                || context.getIssueDate() != null;
    }

    @Override
    public TemporalRuleResult evaluate(TemporalEvaluationContext context) {
        LocalDateTime bidDate = context.getBidSubmissionDate();
        LocalDateTime validFrom = context.getValidFrom();
        LocalDateTime validUntil = context.getValidUntil();
        LocalDateTime issueDate = context.getIssueDate();

        if (validFrom != null && bidDate.toLocalDate().isBefore(validFrom.toLocalDate())) {
            return TemporalRuleResult.notMatched();
        }

        if (validUntil != null && bidDate.toLocalDate().isAfter(validUntil.toLocalDate())) {
            return TemporalRuleResult.notMatched();
        }

        if (validFrom == null && validUntil == null && issueDate != null) {
            if (bidDate.toLocalDate().isBefore(issueDate.toLocalDate())) {
                return TemporalRuleResult.notMatched();
            }
        }

        return TemporalRuleResult.of(TemporalStatus.VALID_AT_BID_DATE, REASON, RULE_NAME, getPriority());
    }

    @Override
    public int getPriority() {
        return 7;
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

}
