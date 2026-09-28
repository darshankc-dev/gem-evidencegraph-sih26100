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
@Table(name = "compliance_requirements", indexes = {
        @Index(name = "idx_comp_req_code", columnList = "requirement_code"),
        @Index(name = "idx_comp_req_tender", columnList = "tender_id")
})
public class ComplianceRequirement extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id", nullable = false)
    private Tender tender;

    @Column(name = "requirement_code", nullable = false)
    private String requirementCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false)
    private RequirementType requirementType;

    @Column(name = "mandatory", nullable = false)
    private boolean mandatory;

    @Enumerated(EnumType.STRING)
    @Column(name = "expected_source_type")
    private SourceType expectedSourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "expected_document_type")
    private DocumentType expectedDocumentType;

    @Column(name = "validation_rule", columnDefinition = "TEXT")
    private String validationRule;

}
