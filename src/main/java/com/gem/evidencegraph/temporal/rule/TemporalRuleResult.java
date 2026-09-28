package com.gem.evidencegraph.temporal.rule;

import com.gem.evidencegraph.temporal.TemporalStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TemporalRuleResult {

    private final boolean matched;
    private final TemporalStatus status;
    private final String reason;
    private final String ruleName;
    private final int priority;

    public static TemporalRuleResult notMatched() {
        return TemporalRuleResult.builder()
                .matched(false)
                .build();
    }

    public static TemporalRuleResult of(TemporalStatus status, String reason, String ruleName, int priority) {
        return TemporalRuleResult.builder()
                .matched(true)
                .status(status)
                .reason(reason)
                .ruleName(ruleName)
                .priority(priority)
                .build();
    }

}
