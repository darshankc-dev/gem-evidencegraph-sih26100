package com.gem.evidencegraph.dto;

import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResultDto {

    private SourceType sourceType;
    private String sourceReference;
    private String sourceSystem;
    private String adapterVersion;
    private VerificationStatus verificationStatus;
    private String subject;
    private String attribute;
    private String expectedValue;
    private String observedValue;
    private String normalizedObservedValue;
    private Double confidence;
    private LocalDateTime observedAt;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String rawResponseSnapshot;
    private String reason;

}
