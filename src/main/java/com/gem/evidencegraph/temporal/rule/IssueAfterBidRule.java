package com.gem.evidencegraph.temporal.rule;

import com.gem.evidencegraph.temporal.TemporalEvaluationContext;
import com.gem.evidencegraph.temporal.TemporalStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(50)
public class IssueAfterBidRule implements TemporalRule {

    public static final String RULE_NAME = "IssueAfterBidRule";
    public static final String REASON = "Evidence issue date occurs after the bid submission date.";

    @Override
    public boolean supports(TemporalEvaluationContext context) {
        return context != null
                && context.getIssueDate() != null
                && context.getBidSubmissionDate() != null;
    }

    @Override
    public TemporalRuleResult evaluate(TemporalEvaluationContext context) {
        LocalDateTime issueDate = context.getIssueDate();
        LocalDateTime bidDate = context.getBidSubmissionDate();

        if (issueDate.toLocalDate().isAfter(bidDate.toLocalDate())) {
            return TemporalRuleResult.of(TemporalStatus.ISSUED_AFTER_BID, REASON, RULE_NAME, getPriority());
        }

        return TemporalRuleResult.notMatched();
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

}
