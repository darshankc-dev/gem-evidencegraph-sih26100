package com.gem.evidencegraph.entity;

import com.gem.evidencegraph.risk.RiskDimension;
import com.gem.evidencegraph.risk.RiskSeverity;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "risk_findings", indexes = {
        @Index(name = "idx_rf_assessment_id", columnList = "risk_assessment_id"),
        @Index(name = "idx_rf_bid_id", columnList = "bid_id"),
        @Index(name = "idx_rf_dimension", columnList = "dimension"),
        @Index(name = "idx_rf_severity", columnList = "severity"),
        @Index(name = "idx_rf_code", columnList = "code")
})
public class RiskFinding extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_assessment_id", nullable = false)
    private RiskAssessment riskAssessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id")
    private Bid bid;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimension", nullable = false, length = 50)
    private RiskDimension dimension;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private RiskSeverity severity;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_id", length = 255)
    private String sourceId;

    @Column(name = "rule_name", length = 100)
    private String rule;

    @Column(name = "weight", nullable = false)
    private Integer weight;

}
