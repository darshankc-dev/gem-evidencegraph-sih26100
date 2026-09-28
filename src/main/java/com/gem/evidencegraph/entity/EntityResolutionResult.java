package com.gem.evidencegraph.entity;

import com.gem.evidencegraph.entityresolution.EntityMatchStatus;
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
@Table(name = "entity_resolution_results", indexes = {
        @Index(name = "idx_err_bidder_id", columnList = "bidder_id"),
        @Index(name = "idx_err_match_status", columnList = "match_status")
})
public class EntityResolutionResult extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id")
    private Bidder bidder;

    @Column(name = "compared_source", length = 100)
    private String comparedSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 30)
    private EntityMatchStatus matchStatus;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "matched_attributes", length = 500)
    private String matchedAttributes;

    @Column(name = "mismatched_attributes", length = 500)
    private String mismatchedAttributes;

    @Column(name = "missing_attributes", length = 500)
    private String missingAttributes;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

}
