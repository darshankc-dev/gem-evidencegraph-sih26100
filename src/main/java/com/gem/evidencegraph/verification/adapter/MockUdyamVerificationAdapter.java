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
public class MockUdyamVerificationAdapter implements VerificationAdapter {

    public static final String ADAPTER_VERSION = "mock-udyam-v1.0";
    public static final String SOURCE_SYSTEM = "MSME-UDYAM-MOCK";

    private final Map<String, VerificationResultDto> mockResponses = new ConcurrentHashMap<>();
    private volatile boolean simulateUnavailable = false;

    @Override
    public SourceType getSourceType() {
        return SourceType.UDYAM;
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
        return "UDYAM_NUMBER".equals(field) || "UDYAM".equals(field);
    }

    @Override
    public VerificationResultDto verify(Claim claim) {
        if (simulateUnavailable) {
            return VerificationResultDto.builder()
                    .sourceType(SourceType.UDYAM)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("UDYAM-ERR-TIMEOUT")
                    .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                    .subject("UDYAM_NUMBER")
                    .attribute("UDYAM_NUMBER")
                    .expectedValue(claim.getNormalizedValue())
                    .confidence(0.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot("{\"error\": \"Udyam server unreachable\"}")
                    .reason("Udyam portal unavailable or timed out")
                    .build();
        }

        String expected = claim.getNormalizedValue() != null ? claim.getNormalizedValue().trim() : "";

        if (mockResponses.containsKey(expected)) {
            return mockResponses.get(expected);
        }

        if (expected.contains("UNAVAILABLE")) {
            return VerificationResultDto.builder()
                    .sourceType(SourceType.UDYAM)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("UDYAM-MOCK-UNAVAILABLE")
                    .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                    .subject("UDYAM_NUMBER")
                    .attribute("UDYAM_NUMBER")
                    .expectedValue(expected)
                    .confidence(0.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot("{\"error\": \"MSME service unavailable\"}")
                    .reason("MSME Udyam service unreachable")
                    .build();
        }

        if (expected.contains("UNVERIFIED")) {
            return VerificationResultDto.builder()
                    .sourceType(SourceType.UDYAM)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("UDYAM-NOT-FOUND")
                    .verificationStatus(VerificationStatus.UNVERIFIED)
                    .subject("UDYAM_NUMBER")
                    .attribute("UDYAM_NUMBER")
                    .expectedValue(expected)
                    .confidence(0.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot("{\"status\": \"NOT_FOUND\"}")
                    .reason("No registration found in MSME Udyam registry")
                    .build();
        }

        if (expected.contains("MISMATCH")) {
            String observedMismatched = "UDYAM-XX-00-9999999";
            return VerificationResultDto.builder()
                    .sourceType(SourceType.UDYAM)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("UDYAM-REF-MISMATCH")
                    .verificationStatus(VerificationStatus.MISMATCH)
                    .subject("UDYAM_NUMBER")
                    .attribute("UDYAM_NUMBER")
                    .expectedValue(expected)
                    .observedValue(observedMismatched)
                    .normalizedObservedValue(observedMismatched)
                    .confidence(1.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot(String.format("{\"udyamRegistrationNo\": \"%s\", \"status\": \"ACTIVE\"}", observedMismatched))
                    .reason("Observed Udyam number conflicts with claimed value")
                    .build();
        }

        return VerificationResultDto.builder()
                .sourceType(SourceType.UDYAM)
                .sourceSystem(SOURCE_SYSTEM)
                .adapterVersion(ADAPTER_VERSION)
                .sourceReference("MSME-UDYAM-" + expected)
                .verificationStatus(VerificationStatus.VERIFIED)
                .subject("UDYAM_NUMBER")
                .attribute("UDYAM_NUMBER")
                .expectedValue(expected)
                .observedValue(expected)
                .normalizedObservedValue(expected)
                .confidence(1.0)
                .observedAt(LocalDateTime.now())
                .validFrom(LocalDateTime.now().minusYears(1))
                .rawResponseSnapshot(String.format("{\"udyamRegistrationNo\": \"%s\", \"enterpriseType\": \"MICRO\", \"status\": \"VERIFIED\"}", expected))
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
