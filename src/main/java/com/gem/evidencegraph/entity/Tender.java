package com.gem.evidencegraph.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "tenders", indexes = {
        @Index(name = "idx_tender_reference", columnList = "tender_reference")
})
public class Tender extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tender_reference", nullable = false, unique = true)
    private String tenderReference;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "publication_date")
    private LocalDateTime publicationDate;

    @Column(name = "submission_deadline")
    private LocalDateTime submissionDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenderStatus status;

}
