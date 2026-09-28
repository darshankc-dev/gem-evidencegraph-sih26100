package com.gem.evidencegraph.risk;

import java.util.Map;

public final class RiskConstants {

    private RiskConstants() {

    }

    public static final int WEIGHT_INFO = 0;
    public static final int WEIGHT_LOW = 10;
    public static final int WEIGHT_MEDIUM = 25;
    public static final int WEIGHT_HIGH = 50;
    public static final int WEIGHT_CRITICAL = 100;

    private static final Map<RiskSeverity, Integer> SEVERITY_WEIGHTS = Map.of(
            RiskSeverity.INFO, WEIGHT_INFO,
            RiskSeverity.LOW, WEIGHT_LOW,
            RiskSeverity.MEDIUM, WEIGHT_MEDIUM,
            RiskSeverity.HIGH, WEIGHT_HIGH,
            RiskSeverity.CRITICAL, WEIGHT_CRITICAL
    );

    public static final int THRESHOLD_LOW_MAX = 24;
    public static final int THRESHOLD_MEDIUM_MIN = 25;
    public static final int THRESHOLD_MEDIUM_MAX = 59;
    public static final int THRESHOLD_HIGH_MIN = 60;
    public static final int SCORE_CAP = 100;

    public static int getWeight(RiskSeverity severity) {
        if (severity == null) {
            return WEIGHT_INFO;
        }
        return SEVERITY_WEIGHTS.getOrDefault(severity, WEIGHT_INFO);
    }

    public static int capScore(int sum) {
        if (sum <= 0) {
            return 0;
        }
        return Math.min(SCORE_CAP, sum);
    }
}
