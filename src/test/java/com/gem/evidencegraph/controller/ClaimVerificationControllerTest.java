package com.gem.evidencegraph.controller;

import com.gem.evidencegraph.config.GlobalExceptionHandler;
import com.gem.evidencegraph.dto.BulkVerificationResponseDto;
import com.gem.evidencegraph.dto.VerificationRequestDto;
import com.gem.evidencegraph.dto.VerificationResponseDto;
import com.gem.evidencegraph.entity.SourceType;
import com.gem.evidencegraph.entity.VerificationStatus;
import com.gem.evidencegraph.security.SecurityConfig;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import com.gem.evidencegraph.verification.gateway.VerificationGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimVerificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ClaimVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VerificationGateway verificationGateway;

    @Test
    @DisplayName("POST /api/claims/{claimId}/verify should return 200 with verification response")
    void shouldVerifyClaimSuccessfully() throws Exception {
        UUID claimId = UUID.randomUUID();
        UUID evidenceId = UUID.randomUUID();
        UUID relId = UUID.randomUUID();

        VerificationResponseDto responseDto = VerificationResponseDto.builder()
                .claimId(claimId)
                .sourceType(SourceType.GST)
                .verificationStatus(VerificationStatus.VERIFIED)
                .expectedValue("29ABCDE1234F1Z5")
                .observedValue("29ABCDE1234F1Z5")
                .evidenceId(evidenceId)
                .relationshipId(relId)
                .confidence(1.0)
                .reason("Claim value matches source evidence.")
                .build();

        when(verificationGateway.verifyClaim(any())).thenReturn(responseDto);

        VerificationRequestDto requestDto = VerificationRequestDto.builder()
                .requestedSourceType(SourceType.GST)
                .build();

        mockMvc.perform(post("/api/claims/{claimId}/verify", claimId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(claimId.toString()))
                .andExpect(jsonPath("$.sourceType").value("GST"))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.expectedValue").value("29ABCDE1234F1Z5"))
                .andExpect(jsonPath("$.evidenceId").value(evidenceId.toString()))
                .andExpect(jsonPath("$.confidence").value(1.0));
    }

    @Test
    @DisplayName("POST /api/claims/{claimId}/verify should return 404 when claim not found")
    void shouldReturn404WhenClaimNotFound() throws Exception {
        UUID claimId = UUID.randomUUID();
        when(verificationGateway.verifyClaim(any()))
                .thenThrow(new ResourceNotFoundException("Claim with ID '" + claimId + "' not found"));

        mockMvc.perform(post("/api/claims/{claimId}/verify", claimId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Claim with ID '" + claimId + "' not found"));
    }

    @Test
    @DisplayName("POST /api/bids/{bidId}/verify should return 200 with bulk verification response")
    void shouldBulkVerifyBidClaims() throws Exception {
        UUID bidId = UUID.randomUUID();
        BulkVerificationResponseDto bulkResponse = BulkVerificationResponseDto.builder()
                .bidId(bidId)
                .totalClaimsScanned(2)
                .verifiedCount(2)
                .mismatchCount(0)
                .unverifiedCount(0)
                .unavailableCount(0)
                .results(List.of())
                .build();

        when(verificationGateway.verifyBidClaims(eq(bidId), any())).thenReturn(bulkResponse);

        mockMvc.perform(post("/api/bids/{bidId}/verify", bidId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bidId").value(bidId.toString()))
                .andExpect(jsonPath("$.totalClaimsScanned").value(2))
                .andExpect(jsonPath("$.verifiedCount").value(2));
    }

}
