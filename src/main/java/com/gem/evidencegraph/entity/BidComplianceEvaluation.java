package com.gem.evidencegraph.entity;

import com.gem.evidencegraph.compliance.OverallComplianceStatus;
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
@Table(name = "bid_compliance_evaluations", indexes = {
        @Index(name = "idx_bce_bid_id", columnList = "bid_id"),
        @Index(name = "idx_bce_overall_status", columnList = "overall_status")
})
public class BidComplianceEvaluation extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false)
    private Bid bid;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false, length = 30)
    private OverallComplianceStatus overallStatus;

    @Column(name = "compliance_percentage")
    private Double compliancePercentage;

    @Column(name = "mandatory_requirement_count")
    private int mandatoryRequirementCount;

    @Column(name = "compliant_count")
    private int compliantCount;

    @Column(name = "non_compliant_count")
    private int nonCompliantCount;

    @Column(name = "missing_count")
    private int missingCount;

    @Column(name = "unverified_count")
    private int unverifiedCount;

    @Column(name = "contradictory_count")
    private int contradictoryCount;

    @Column(name = "pending_review_count")
    private int pendingReviewCount;

    @Column(name = "not_applicable_count")
    private int notApplicableCount;

    @Column(name = "evaluation_details_json", columnDefinition = "TEXT")
    private String evaluationDetailsJson;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

}
