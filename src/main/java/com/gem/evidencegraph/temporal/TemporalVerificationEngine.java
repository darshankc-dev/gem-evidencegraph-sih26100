package com.gem.evidencegraph.temporal;

import com.gem.evidencegraph.temporal.rule.TemporalRule;
import com.gem.evidencegraph.temporal.rule.TemporalRuleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class TemporalVerificationEngine {

    private final List<TemporalRule> rules;

    public TemporalVerificationEngine(List<TemporalRule> rules) {
        this.rules = rules != null
                ? rules.stream().sorted(Comparator.comparingInt(TemporalRule::getPriority)).toList()
                : List.of();
    }

    public TemporalEvaluationOutcome evaluate(TemporalEvaluationContext context) {
        if (context == null) {
            throw new IllegalArgumentException("TemporalEvaluationContext must not be null");
        }

        List<TemporalRuleResult> findings = new ArrayList<>();

        for (TemporalRule rule : rules) {
            if (rule.supports(context)) {
                TemporalRuleResult result = rule.evaluate(context);
                if (result != null && result.isMatched()) {
                    findings.add(result);
                }
            }
        }

        if (findings.isEmpty()) {
            return TemporalEvaluationOutcome.builder()
                    .primaryStatus(TemporalStatus.NOT_APPLICABLE)
                    .reason("Evidence does not require temporal evaluation.")
                    .ruleApplied("NotApplicableRule")
                    .allFindings(List.of())
                    .build();
        }

        TemporalRuleResult primary = findings.get(0);

        return TemporalEvaluationOutcome.builder()
                .primaryStatus(primary.getStatus())
                .reason(primary.getReason())
                .ruleApplied(primary.getRuleName())
                .allFindings(findings)
                .build();
    }

}
