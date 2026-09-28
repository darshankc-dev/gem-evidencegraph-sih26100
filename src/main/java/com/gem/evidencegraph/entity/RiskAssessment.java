package com.gem.evidencegraph.entity;

import com.gem.evidencegraph.risk.RiskDimension;
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
@Table(name = "risk_assessments", indexes = {
        @Index(name = "idx_ra_bid_id", columnList = "bid_id"),
        @Index(name = "idx_ra_bra_id", columnList = "bid_risk_assessment_id"),
        @Index(name = "idx_ra_dimension", columnList = "dimension")
})
public class RiskAssessment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false)
    private Bid bid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_risk_assessment_id")
    private BidRiskAssessment bidRiskAssessment;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimension", nullable = false, length = 50)
    private RiskDimension dimension;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 30)
    private RiskLevel riskLevel;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(name = "finding_count", nullable = false)
    private Integer findingCount;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @OneToMany(mappedBy = "riskAssessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RiskFinding> findings = new ArrayList<>();

}
