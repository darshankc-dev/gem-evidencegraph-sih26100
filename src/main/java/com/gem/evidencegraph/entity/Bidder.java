package com.gem.evidencegraph.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bidders", indexes = {
        @Index(name = "idx_bidder_pan", columnList = "pan"),
        @Index(name = "idx_bidder_gstin", columnList = "gstin"),
        @Index(name = "idx_bidder_udyam", columnList = "udyam_number"),
        @Index(name = "idx_bidder_normalized_name", columnList = "normalized_name")
})
public class Bidder extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "normalized_name")
    private String normalizedName;

    @Column(name = "pan", length = 20)
    private String pan;

    @Column(name = "gstin", length = 30)
    private String gstin;

    @Column(name = "udyam_number", length = 50)
    private String udyamNumber;

    @Column(name = "registered_address", columnDefinition = "TEXT")
    private String registeredAddress;

    @Column(name = "normalized_address", columnDefinition = "TEXT")
    private String normalizedAddress;

    @Column(name = "email")
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

}
