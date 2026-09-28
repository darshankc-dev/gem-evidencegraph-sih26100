package com.gem.evidencegraph.risk;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    public static RiskLevel fromScore(int score) {
        if (score < 25) {
            return LOW;
        } else if (score < 60) {
            return MEDIUM;
        } else {
            return HIGH;
        }
    }
}
