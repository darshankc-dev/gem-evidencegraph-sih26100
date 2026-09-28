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

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "evidences", indexes = {
        @Index(name = "idx_evidence_claim_id", columnList = "claim_id"),
        @Index(name = "idx_evidence_source_type", columnList = "source_type"),
        @Index(name = "idx_evidence_verif_status", columnList = "verification_status"),
        @Index(name = "idx_evidence_normalized_val", columnList = "normalized_value")
})
public class Evidence extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id")
    private Bid bid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    private Claim claim;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false)
    private EvidenceType evidenceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "source_reference")
    private String sourceReference;

    @Column(name = "source_system")
    private String sourceSystem;

    @Column(name = "adapter_version")
    private String adapterVersion;

    @Column(name = "subject")
    private String subject;

    @Column(name = "attribute")
    private String attribute;

    @Column(name = "\"value\"", columnDefinition = "TEXT")
    private String value;

    @Column(name = "normalized_value", columnDefinition = "TEXT")
    private String normalizedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus;

    @Column(name = "observed_at")
    private LocalDateTime observedAt;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "retrieved_at")
    private LocalDateTime retrievedAt;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "provenance_hash", length = 64)
    private String provenanceHash;

    @Column(name = "raw_response_snapshot", columnDefinition = "TEXT")
    private String rawResponseSnapshot;

}
