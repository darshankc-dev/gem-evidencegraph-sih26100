package com.gem.evidencegraph.entity;

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
@Table(name = "evidence_relationships", indexes = {
        @Index(name = "idx_ev_rel_claim", columnList = "source_claim_id"),
        @Index(name = "idx_ev_rel_source", columnList = "source_evidence_id"),
        @Index(name = "idx_ev_rel_target", columnList = "target_evidence_id"),
        @Index(name = "idx_ev_rel_type", columnList = "relationship_type")
})
public class EvidenceRelationship extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_claim_id")
    private Claim sourceClaim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_evidence_id")
    private Evidence sourceEvidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_evidence_id", nullable = false)
    private Evidence targetEvidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false)
    private RelationshipType relationshipType;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "confidence")
    private Double confidence;

}
