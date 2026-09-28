package com.gem.evidencegraph.entity;

import com.gem.evidencegraph.temporal.TemporalStatus;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "temporal_evaluations", indexes = {
        @Index(name = "idx_temp_eval_bid", columnList = "bid_id"),
        @Index(name = "idx_temp_eval_tender", columnList = "tender_id"),
        @Index(name = "idx_temp_eval_evidence", columnList = "evidence_id"),
        @Index(name = "idx_temp_eval_status", columnList = "status")
})
public class TemporalEvaluation extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false)
    private Bid bid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_id", nullable = false)
    private Evidence evidence;

    @Column(name = "reference_date")
    private LocalDateTime referenceDate;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "observed_at")
    private LocalDateTime observedAt;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TemporalStatus status;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "rule_applied", length = 100)
    private String ruleApplied;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

}
