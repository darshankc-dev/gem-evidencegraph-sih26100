package com.gem.evidencegraph.temporal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gem.evidencegraph.config.GlobalExceptionHandler;
import com.gem.evidencegraph.security.SecurityConfig;
import com.gem.evidencegraph.temporal.dto.BidTemporalEvaluationResultDto;
import com.gem.evidencegraph.temporal.dto.TemporalEvaluationResponseDto;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TemporalVerificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class TemporalVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TemporalService temporalService;

    @Test
    @DisplayName("POST /api/bids/{bidId}/temporal/evaluate returns 200 and evaluation result")
    void evaluateBidTemporal_success() throws Exception {
        UUID bidId = UUID.randomUUID();
        UUID tenderId = UUID.randomUUID();
        UUID evidenceId = UUID.randomUUID();

        TemporalEvaluationResponseDto item = TemporalEvaluationResponseDto.builder()
                .evaluationId(UUID.randomUUID())
                .bidId(bidId)
                .tenderId(tenderId)
                .evidenceId(evidenceId)
                .referenceDate(LocalDateTime.of(2026, 9, 20, 10, 0))
                .validFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .validUntil(LocalDateTime.of(2026, 12, 31, 0, 0))
                .status(TemporalStatus.VALID_AT_BID_DATE)
                .reason("Evidence validity window covers the bid submission date.")
                .evaluatedAt(LocalDateTime.now())
                .build();

        BidTemporalEvaluationResultDto responseDto = BidTemporalEvaluationResultDto.builder()
                .bidId(bidId)
                .tenderId(tenderId)
                .evaluatedAt(LocalDateTime.now())
                .totalEvaluated(1)
                .validCount(1)
                .expiredCount(0)
                .conflictingCount(0)
                .results(List.of(item))
                .build();

        when(temporalService.evaluateBidTemporal(bidId)).thenReturn(responseDto);

        mockMvc.perform(post("/api/bids/{bidId}/temporal/evaluate", bidId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bidId").value(bidId.toString()))
                .andExpect(jsonPath("$.totalEvaluated").value(1))
                .andExpect(jsonPath("$.validCount").value(1))
                .andExpect(jsonPath("$.expiredCount").value(0))
                .andExpect(jsonPath("$.results[0].status").value("VALID_AT_BID_DATE"))
                .andExpect(jsonPath("$.results[0].reason").value("Evidence validity window covers the bid submission date."));
    }

    @Test
    @DisplayName("GET /api/bids/{bidId}/temporal returns 200 and stored evaluation")
    void getBidTemporal_success() throws Exception {
        UUID bidId = UUID.randomUUID();
        BidTemporalEvaluationResultDto responseDto = BidTemporalEvaluationResultDto.builder()
                .bidId(bidId)
                .totalEvaluated(0)
                .validCount(0)
                .results(List.of())
                .build();

        when(temporalService.getBidTemporal(bidId)).thenReturn(responseDto);

        mockMvc.perform(get("/api/bids/{bidId}/temporal", bidId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bidId").value(bidId.toString()))
                .andExpect(jsonPath("$.totalEvaluated").value(0));
    }

    @Test
    @DisplayName("GET /api/evidence/{evidenceId}/temporal returns 200 and evaluations list")
    void getEvidenceTemporal_success() throws Exception {
        UUID evidenceId = UUID.randomUUID();
        UUID bidId = UUID.randomUUID();

        TemporalEvaluationResponseDto item = TemporalEvaluationResponseDto.builder()
                .evaluationId(UUID.randomUUID())
                .bidId(bidId)
                .evidenceId(evidenceId)
                .status(TemporalStatus.EXPIRED_AT_BID_DATE)
                .reason("Evidence expired before the bid submission date.")
                .evaluatedAt(LocalDateTime.now())
                .build();

        when(temporalService.getEvidenceTemporal(evidenceId)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/evidence/{evidenceId}/temporal", evidenceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evidenceId").value(evidenceId.toString()))
                .andExpect(jsonPath("$[0].status").value("EXPIRED_AT_BID_DATE"))
                .andExpect(jsonPath("$[0].reason").value("Evidence expired before the bid submission date."));
    }

    @Test
    @DisplayName("POST /api/bids/{bidId}/temporal/evaluate returns 404 when bid is not found")
    void evaluateBidTemporal_notFound() throws Exception {
        UUID bidId = UUID.randomUUID();
        when(temporalService.evaluateBidTemporal(bidId))
                .thenThrow(new ResourceNotFoundException("Bid with ID '" + bidId + "' not found"));

        mockMvc.perform(post("/api/bids/{bidId}/temporal/evaluate", bidId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Bid with ID '" + bidId + "' not found"));
    }

    @Test
    @DisplayName("GET /api/evidence/{evidenceId}/temporal returns 404 when evidence is not found")
    void getEvidenceTemporal_notFound() throws Exception {
        UUID evidenceId = UUID.randomUUID();
        when(temporalService.getEvidenceTemporal(evidenceId))
                .thenThrow(new ResourceNotFoundException("Evidence with ID '" + evidenceId + "' not found"));

        mockMvc.perform(get("/api/evidence/{evidenceId}/temporal", evidenceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Evidence with ID '" + evidenceId + "' not found"));
    }

}
