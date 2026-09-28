package com.gem.evidencegraph.verification.adapter;

import com.gem.evidencegraph.dto.VerificationResultDto;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.VerificationStatus;
import com.gem.evidencegraph.verification.VerificationAdapter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockGstVerificationAdapter implements VerificationAdapter {

    public static final String ADAPTER_VERSION = "mock-gst-v1.0";
    public static final String SOURCE_SYSTEM = "GSTN-PORTAL-MOCK";

    private final Map<String, VerificationResultDto> mockResponses = new ConcurrentHashMap<>();
    private volatile boolean simulateUnavailable = false;

    @Override
    public SourceType getSourceType() {
        return SourceType.GST;
    }

    @Override
    public String getAdapterVersion() {
        return ADAPTER_VERSION;
    }

    @Override
    public boolean supports(Claim claim) {
        if (claim == null || claim.getFieldName() == null) {
            return false;
        }
        String field = claim.getFieldName().toUpperCase();
        return "GSTIN".equals(field) || "LEGAL_NAME".equals(field) || "ADDRESS".equals(field);
    }

    @Override
    public VerificationResultDto verify(Claim claim) {
        if (simulateUnavailable) {
            return VerificationResultDto.builder()
                    .sourceType(SourceType.GST)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("GSTN-ERR-TIMEOUT")
                    .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                    .subject(claim.getFieldName())
                    .attribute(claim.getFieldName())
                    .expectedValue(claim.getNormalizedValue())
                    .observedValue(null)
                    .confidence(0.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot("{\"error\": \"GSTN gateway timeout - service unavailable\"}")
                    .reason("GST portal unavailable or timed out")
                    .build();
        }

        String expected = claim.getNormalizedValue() != null ? claim.getNormalizedValue().trim() : "";

        if (mockResponses.containsKey(expected)) {
            return mockResponses.get(expected);
        }

        if (expected.contains("UNAVAILABLE")) {
            return VerificationResultDto.builder()
                    .sourceType(SourceType.GST)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("GSTN-MOCK-UNAVAILABLE")
                    .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                    .subject(claim.getFieldName())
                    .attribute(claim.getFieldName())
                    .expectedValue(expected)
                    .confidence(0.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot("{\"error\": \"GSTN service unavailable\"}")
                    .reason("GSTN external service unreachable")
                    .build();
        }

        if (expected.contains("UNVERIFIED")) {
            return VerificationResultDto.builder()
                    .sourceType(SourceType.GST)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("GSTN-NOT-FOUND")
                    .verificationStatus(VerificationStatus.UNVERIFIED)
                    .subject(claim.getFieldName())
                    .attribute(claim.getFieldName())
                    .expectedValue(expected)
                    .confidence(0.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot("{\"status\": \"RECORD_NOT_FOUND\"}")
                    .reason("No record found in GSTN registry")
                    .build();
        }

        if (expected.contains("MISMATCH")) {
            String observedMismatched = expected.replace("MISMATCH", "OBSERVED");
            return VerificationResultDto.builder()
                    .sourceType(SourceType.GST)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("GSTN-REF-MISMATCH")
                    .verificationStatus(VerificationStatus.MISMATCH)
                    .subject(claim.getFieldName())
                    .attribute(claim.getFieldName())
                    .expectedValue(expected)
                    .observedValue(observedMismatched)
                    .normalizedObservedValue(observedMismatched)
                    .confidence(1.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot(String.format("{\"gstin\": \"%s\", \"status\": \"ACTIVE\"}", observedMismatched))
                    .reason("Observed GSTIN from registry conflicts with claimed value")
                    .build();
        }

        return VerificationResultDto.builder()
                .sourceType(SourceType.GST)
                .sourceSystem(SOURCE_SYSTEM)
                .adapterVersion(ADAPTER_VERSION)
                .sourceReference("GSTN-REG-" + expected)
                .verificationStatus(VerificationStatus.VERIFIED)
                .subject(claim.getFieldName())
                .attribute(claim.getFieldName())
                .expectedValue(expected)
                .observedValue(expected)
                .normalizedObservedValue(expected)
                .confidence(1.0)
                .observedAt(LocalDateTime.now())
                .validFrom(LocalDateTime.now().minusYears(2))
                .validUntil(LocalDateTime.now().plusYears(5))
                .rawResponseSnapshot(String.format("{\"gstin\": \"%s\", \"status\": \"ACTIVE\", \"taxpayerType\": \"REGULAR\"}", expected))
                .reason("Claim value matches source evidence.")
                .build();
    }

    public void registerMockResponse(String expectedValue, VerificationResultDto result) {
        mockResponses.put(expectedValue, result);
    }

    public void setSimulateUnavailable(boolean simulateUnavailable) {
        this.simulateUnavailable = simulateUnavailable;
    }

    public void clearMockResponses() {
        mockResponses.clear();
        simulateUnavailable = false;
    }

}
