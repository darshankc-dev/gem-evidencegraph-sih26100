package com.gem.evidencegraph.risk.evaluator;

import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.BidRiskAssessment;
import com.gem.evidencegraph.entity.RiskAssessment;
import com.gem.evidencegraph.entity.RiskFinding;
import com.gem.evidencegraph.risk.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiskAssessmentEngine {

    private final EligibilityRiskEvaluator eligibilityRiskEvaluator;
    private final DocumentIntegrityRiskEvaluator documentIntegrityRiskEvaluator;
    private final ConsistencyRiskEvaluator consistencyRiskEvaluator;
    private final TemporalRiskEvaluator temporalRiskEvaluator;
    private final VerificationRiskEvaluator verificationRiskEvaluator;
    private final HardFailEvaluator hardFailEvaluator;
    private final DecisionEngine decisionEngine;

    public BidRiskAssessment evaluate(RiskEvaluationContext context) {
        Bid bid = context.getBid();
        LocalDateTime evaluatedAt = context.getEvaluatedAt() != null ? context.getEvaluatedAt() : LocalDateTime.now();

        DimensionEvaluationResult eligibilityResult = eligibilityRiskEvaluator.evaluate(context);
        DimensionEvaluationResult docIntegrityResult = documentIntegrityRiskEvaluator.evaluate(context);
        DimensionEvaluationResult consistencyResult = consistencyRiskEvaluator.evaluate(context);
        DimensionEvaluationResult temporalResult = temporalRiskEvaluator.evaluate(context);
        DimensionEvaluationResult verificationResult = verificationRiskEvaluator.evaluate(context);

        List<DimensionEvaluationResult> dimensionResults = List.of(
                eligibilityResult,
                docIntegrityResult,
                consistencyResult,
                temporalResult,
                verificationResult
        );

        List<RiskFinding> allFindings = new ArrayList<>();
        dimensionResults.forEach(dr -> allFindings.addAll(dr.getFindings()));

        int overallRiskScore = dimensionResults.stream()
                .mapToInt(DimensionEvaluationResult::getRiskScore)
                .max()
                .orElse(0);

        RiskLevel overallRiskLevel = RiskLevel.fromScore(overallRiskScore);

        HardFailResult hardFailResult = hardFailEvaluator.evaluate(context, allFindings);

        DecisionOutcome decisionOutcome = decisionEngine.evaluate(
                context, hardFailResult, allFindings, overallRiskScore, overallRiskLevel);

        BidRiskAssessment bidRiskAssessment = BidRiskAssessment.builder()
                .bid(bid)
                .overallRiskLevel(overallRiskLevel)
                .overallRiskScore(overallRiskScore)
                .decisionRecommendation(decisionOutcome.getRecommendation())
                .hardFail(hardFailResult.isHardFail())
                .summary(decisionOutcome.getSummary())
                .reason(decisionOutcome.getReason())
                .evaluatedAt(evaluatedAt)
                .build();

        List<RiskAssessment> assessments = new ArrayList<>();
        for (DimensionEvaluationResult dr : dimensionResults) {
            RiskAssessment ra = RiskAssessment.builder()
                    .bid(bid)
                    .bidRiskAssessment(bidRiskAssessment)
                    .dimension(dr.getDimension())
                    .riskLevel(dr.getRiskLevel())
                    .riskScore(dr.getRiskScore())
                    .findingCount(dr.getFindingCount())
                    .summary(dr.getSummary())
                    .reason(dr.getReason())
                    .evaluatedAt(evaluatedAt)
                    .build();

            for (RiskFinding finding : dr.getFindings()) {
                finding.setRiskAssessment(ra);
                finding.setBid(bid);
                ra.getFindings().add(finding);
            }

            assessments.add(ra);
        }

        bidRiskAssessment.setDimensionAssessments(assessments);

        log.info("Completed risk assessment for bid {}: overallScore={}, overallLevel={}, recommendation={}, hardFail={}",
                bid.getBidReference(), overallRiskScore, overallRiskLevel,
                decisionOutcome.getRecommendation(), hardFailResult.isHardFail());

        return bidRiskAssessment;
    }

}
