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
public class MockPanVerificationAdapter implements VerificationAdapter {

    public static final String ADAPTER_VERSION = "mock-pan-v1.0";
    public static final String SOURCE_SYSTEM = "ITD-NSDL-MOCK";

    private final Map<String, VerificationResultDto> mockResponses = new ConcurrentHashMap<>();
    private volatile boolean simulateUnavailable = false;

    @Override
    public SourceType getSourceType() {
        return SourceType.PAN;
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
        return "PAN".equalsIgnoreCase(claim.getFieldName());
    }

    @Override
    public VerificationResultDto verify(Claim claim) {
        if (simulateUnavailable) {
            return VerificationResultDto.builder()
                    .sourceType(SourceType.PAN)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("PAN-ERR-TIMEOUT")
                    .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                    .subject("PAN")
                    .attribute("PAN")
                    .expectedValue(claim.getNormalizedValue())
                    .confidence(0.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot("{\"error\": \"PAN verification server unreachable\"}")
                    .reason("PAN portal unavailable or timed out")
                    .build();
        }

        String expected = claim.getNormalizedValue() != null ? claim.getNormalizedValue().trim() : "";

        if (mockResponses.containsKey(expected)) {
            return mockResponses.get(expected);
        }

        if (expected.contains("UNAVAILABLE")) {
            return VerificationResultDto.builder()
                    .sourceType(SourceType.PAN)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("PAN-MOCK-UNAVAILABLE")
                    .verificationStatus(VerificationStatus.SOURCE_UNAVAILABLE)
                    .subject("PAN")
                    .attribute("PAN")
                    .expectedValue(expected)
                    .confidence(0.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot("{\"error\": \"ITD service unavailable\"}")
                    .reason("ITD service unreachable")
                    .build();
        }

        if (expected.contains("UNVERIFIED")) {
            return VerificationResultDto.builder()
                    .sourceType(SourceType.PAN)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("PAN-NOT-FOUND")
                    .verificationStatus(VerificationStatus.UNVERIFIED)
                    .subject("PAN")
                    .attribute("PAN")
                    .expectedValue(expected)
                    .confidence(0.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot("{\"panStatus\": \"NOT_FOUND\"}")
                    .reason("No record found in ITD PAN database")
                    .build();
        }

        if (expected.contains("MISMATCH")) {
            String observedMismatched = expected.replace("MISMATCH", "PAN99");
            return VerificationResultDto.builder()
                    .sourceType(SourceType.PAN)
                    .sourceSystem(SOURCE_SYSTEM)
                    .adapterVersion(ADAPTER_VERSION)
                    .sourceReference("PAN-REF-MISMATCH")
                    .verificationStatus(VerificationStatus.MISMATCH)
                    .subject("PAN")
                    .attribute("PAN")
                    .expectedValue(expected)
                    .observedValue(observedMismatched)
                    .normalizedObservedValue(observedMismatched)
                    .confidence(1.0)
                    .observedAt(LocalDateTime.now())
                    .rawResponseSnapshot(String.format("{\"pan\": \"%s\", \"panStatus\": \"OPERATIVE\"}", observedMismatched))
                    .reason("Observed PAN from NSDL conflicts with claimed value")
                    .build();
        }

        return VerificationResultDto.builder()
                .sourceType(SourceType.PAN)
                .sourceSystem(SOURCE_SYSTEM)
                .adapterVersion(ADAPTER_VERSION)
                .sourceReference("ITD-PAN-" + expected)
                .verificationStatus(VerificationStatus.VERIFIED)
                .subject("PAN")
                .attribute("PAN")
                .expectedValue(expected)
                .observedValue(expected)
                .normalizedObservedValue(expected)
                .confidence(1.0)
                .observedAt(LocalDateTime.now())
                .rawResponseSnapshot(String.format("{\"pan\": \"%s\", \"panStatus\": \"OPERATIVE\", \"holderCategory\": \"COMPANY\"}", expected))
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
