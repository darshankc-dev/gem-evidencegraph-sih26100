package com.gem.evidencegraph.temporal.rule;

import com.gem.evidencegraph.temporal.TemporalEvaluationContext;
import com.gem.evidencegraph.temporal.TemporalStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(60)
public class IssueAfterTenderPublicationRule implements TemporalRule {

    public static final String RULE_NAME = "IssueAfterTenderPublicationRule";
    public static final String REASON = "Evidence issue date occurs after the tender publication date.";

    @Override
    public boolean supports(TemporalEvaluationContext context) {
        return context != null
                && context.getIssueDate() != null
                && context.getTenderPublicationDate() != null;
    }

    @Override
    public TemporalRuleResult evaluate(TemporalEvaluationContext context) {
        LocalDateTime issueDate = context.getIssueDate();
        LocalDateTime pubDate = context.getTenderPublicationDate();

        if (issueDate.toLocalDate().isAfter(pubDate.toLocalDate())) {
            return TemporalRuleResult.of(TemporalStatus.ISSUED_AFTER_TENDER_PUBLICATION, REASON, RULE_NAME, getPriority());
        }

        return TemporalRuleResult.notMatched();
    }

    @Override
    public int getPriority() {
        return 6;
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

}
