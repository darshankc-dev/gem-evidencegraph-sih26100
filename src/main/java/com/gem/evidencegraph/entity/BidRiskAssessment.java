package com.gem.evidencegraph.entity;

import com.gem.evidencegraph.risk.DecisionRecommendation;
import com.gem.evidencegraph.risk.RiskLevel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bid_risk_assessments", indexes = {
        @Index(name = "idx_bra_bid_id", columnList = "bid_id"),
        @Index(name = "idx_bra_risk_level", columnList = "overall_risk_level"),
        @Index(name = "idx_bra_recommendation", columnList = "decision_recommendation")
})
public class BidRiskAssessment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false)
    private Bid bid;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_risk_level", nullable = false, length = 30)
    private RiskLevel overallRiskLevel;

    @Column(name = "overall_risk_score", nullable = false)
    private Integer overallRiskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_recommendation", nullable = false, length = 50)
    private DecisionRecommendation decisionRecommendation;

    @Column(name = "hard_fail", nullable = false)
    private Boolean hardFail;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @OneToMany(mappedBy = "bidRiskAssessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RiskAssessment> dimensionAssessments = new ArrayList<>();

}
