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
public class MockDebarmentVerificationAdapter implements VerificationAdapter {

    public static final String ADAPTER_VERSION = "mock-debarment-v1.0";
    public static final String SOURCE_SYSTEM = "CPPP-DEBARMENT-MOCK";

    private final Map<String, VerificationResultDto> mockResponses = new ConcurrentHashMap<>();
    private volatile boolean simulateUnavailable = false;

    @Override
    public SourceType getSourceType() {
        return SourceType.DEBARMENT_LIST;
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
        return "PAN".equals(field) || "GSTIN".equals(field) || "LEGAL_NAME".equals(field);
    }

    @Override
    public VerificationResultDto verify(Claim claim) {
        if (simulateUnavailable) {
            return VerificationResultDto.builder()
                    .sourceType(SourceType.DEBARMENT_LIST)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("DEBAR-ERR-TIMEOUT")
                    .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                    .subject(claim.getFieldName())
                    .attribute("DEBARMENT_STATUS")
                    .expectedValue(claim.getNormalizedValue())
                    .confidence(0.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot("{\"error\": \"Debarment register service unreachable\"}")
                    .reason("Debarment register unavailable")
                    .build();
        }

        String expected = claim.getNormalizedValue() != null ? claim.getNormalizedValue().trim() : "";

        if (mockResponses.containsKey(expected)) {
            return mockResponses.get(expected);
        }

        if (expected.contains("UNAVAILABLE")) {
            return VerificationResultDto.builder()
                    .sourceType(SourceType.DEBARMENT_LIST)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("DEBAR-MOCK-UNAVAILABLE")
                    .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                    .subject(claim.getFieldName())
                    .attribute("DEBARMENT_STATUS")
                    .expectedValue(expected)
                    .confidence(0.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot("{\"error\": \"Debarment service unavailable\"}")
                    .reason("CPPP debarment register unreachable")
                    .build();
        }

        if (expected.contains("DEBARRED")) {
            return VerificationResultDto.builder()
                    .sourceType(SourceType.DEBARMENT_LIST)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("CPPP-DEBAR-ORDER-2026")
                    .verificationStatus(VerificationStatus.MISMATCH)
                    .subject(claim.getFieldName())
                    .attribute("DEBARMENT_STATUS")
                    .expectedValue(expected)
                    .observedValue("DEBARRED")
                    .normalizedObservedValue("DEBARRED")
                    .confidence(1.0)
                    .observedAt(LocalDateTime.now())
                    .validFrom(LocalDateTime.now().minusMonths(3))
                    .validUntil(LocalDateTime.now().plusMonths(9))
                    .rawResponseSnapshot(String.format("{\"identifier\": \"%s\", \"debarred\": true, \"reason\": \"Breach of procurement integrity code\"}", expected))
                    .reason("Entity is actively debarred on Central Public Procurement Portal blacklist")
                    .build();
        }

        return VerificationResultDto.builder()
                .sourceType(SourceType.DEBARMENT_LIST)
                .sourceSystem(SOURCE_SYSTEM)
                .adapterVersion(ADAPTER_VERSION)
                .sourceReference("CPPP-DEBAR-CHECK-" + expected)
                .verificationStatus(VerificationStatus.VERIFIED)
                .subject(claim.getFieldName())
                .attribute("DEBARMENT_STATUS")
                .expectedValue(expected)
                .observedValue("NOT_DEBARRED")
                .normalizedObservedValue("NOT_DEBARRED")
                .confidence(1.0)
                .observedAt(LocalDateTime.now())
                .rawResponseSnapshot(String.format("{\"identifier\": \"%s\", \"debarred\": false, \"status\": \"CLEARED\"}", expected))
                .reason("Entity not found on central debarment or blacklist registers")
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
