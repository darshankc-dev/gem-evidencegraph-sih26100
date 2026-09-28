package com.gem.evidencegraph.temporal;

import com.gem.evidencegraph.entity.EvidenceType;
import com.gem.evidencegraph.entity.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporalEvaluationContext {

    private UUID bidId;
    private UUID tenderId;
    private LocalDateTime bidSubmissionDate;
    private LocalDateTime tenderPublicationDate;
    private LocalDateTime tenderSubmissionDeadline;

    private UUID evidenceId;
    private EvidenceType evidenceType;
    private SourceType sourceType;
    private String subject;
    private String attribute;

    private LocalDateTime observedAt;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime issueDate;

    private Boolean requiresTemporalEvaluation;
    private boolean conflictingDatesDetected;

    public boolean isTemporalEvaluationRequired() {
        if (requiresTemporalEvaluation != null) {
            return requiresTemporalEvaluation;
        }

        if (validFrom != null || validUntil != null || issueDate != null) {
            return true;
        }

        String attr = (attribute != null ? attribute : "").toUpperCase();
        String subj = (subject != null ? subject : "").toUpperCase();

        if (attr.contains("CERTIFICATE") || attr.contains("AUTHORIZATION") || attr.contains("OEM")
                || attr.contains("LICENSE") || attr.contains("REGISTRATION") || attr.contains("VALIDITY")
                || attr.contains("EXPIRY") || attr.contains("DEBARMENT") || attr.contains("CLEARANCE")
                || subj.contains("CERTIFICATE") || subj.contains("AUTHORIZATION") || subj.contains("OEM")
                || subj.contains("LICENSE")) {
            return true;
        }

        return false;
    }

    public static class TemporalEvaluationContextBuilder {
        public TemporalEvaluationContextBuilder bidSubmissionDate(LocalDate date) {
            this.bidSubmissionDate = date != null ? date.atStartOfDay() : null;
            return this;
        }

        public TemporalEvaluationContextBuilder bidSubmissionDate(LocalDateTime dateTime) {
            this.bidSubmissionDate = dateTime;
            return this;
        }

        public TemporalEvaluationContextBuilder tenderPublicationDate(LocalDate date) {
            this.tenderPublicationDate = date != null ? date.atStartOfDay() : null;
            return this;
        }

        public TemporalEvaluationContextBuilder tenderPublicationDate(LocalDateTime dateTime) {
            this.tenderPublicationDate = dateTime;
            return this;
        }

        public TemporalEvaluationContextBuilder validFrom(LocalDate date) {
            this.validFrom = date != null ? date.atStartOfDay() : null;
            return this;
        }

        public TemporalEvaluationContextBuilder validFrom(LocalDateTime dateTime) {
            this.validFrom = dateTime;
            return this;
        }

        public TemporalEvaluationContextBuilder validUntil(LocalDate date) {
            this.validUntil = date != null ? date.atStartOfDay() : null;
            return this;
        }

        public TemporalEvaluationContextBuilder validUntil(LocalDateTime dateTime) {
            this.validUntil = dateTime;
            return this;
        }

        public TemporalEvaluationContextBuilder issueDate(LocalDate date) {
            this.issueDate = date != null ? date.atStartOfDay() : null;
            return this;
        }

        public TemporalEvaluationContextBuilder issueDate(LocalDateTime dateTime) {
            this.issueDate = dateTime;
            return this;
        }

        public TemporalEvaluationContextBuilder observedAt(LocalDate date) {
            this.observedAt = date != null ? date.atStartOfDay() : null;
            return this;
        }

        public TemporalEvaluationContextBuilder observedAt(LocalDateTime dateTime) {
            this.observedAt = dateTime;
            return this;
        }
    }

}
