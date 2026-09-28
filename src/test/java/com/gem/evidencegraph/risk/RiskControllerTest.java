package com.gem.evidencegraph.risk;

import com.gem.evidencegraph.config.GlobalExceptionHandler;
import com.gem.evidencegraph.risk.dto.BidRiskAssessmentResponseDto;
import com.gem.evidencegraph.risk.dto.RiskDimensionResultDto;
import com.gem.evidencegraph.risk.dto.RiskFindingDto;
import com.gem.evidencegraph.security.SecurityConfig;
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

@WebMvcTest(RiskController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class RiskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskService riskService;

    @Test
    @DisplayName("POST /api/bids/{bidId}/risk/evaluate returns 200 and BidRiskAssessmentResponseDto")
    void testEvaluateBidRiskEndpoint() throws Exception {
        UUID bidId = UUID.randomUUID();

        RiskFindingDto findingDto = RiskFindingDto.builder()
                .dimension(RiskDimension.ELIGIBILITY)
                .severity(RiskSeverity.INFO)
                .code("ELIGIBILITY-000")
                .title("Requirement satisfied")
                .description("Compliant requirement")
                .sourceType("COMPLIANCE_REQUIREMENT")
                .sourceId("REQ-001")
                .rule("RULE-ELIGIBILITY-000")
                .weight(0)
                .build();

        RiskDimensionResultDto dimDto = RiskDimensionResultDto.builder()
                .dimension(RiskDimension.ELIGIBILITY)
                .riskLevel(RiskLevel.LOW)
                .riskScore(0)
                .findingCount(1)
                .summary("Eligibility evaluated with score 0 (LOW)")
                .findings(List.of(findingDto))
                .build();

        BidRiskAssessmentResponseDto response = BidRiskAssessmentResponseDto.builder()
                .bidId(bidId)
                .overallRiskLevel(RiskLevel.LOW)
                .overallRiskScore(0)
                .decisionRecommendation(DecisionRecommendation.RECOMMEND)
                .hardFail(false)
                .summary("Recommendation: RECOMMEND")
                .reason("Checks satisfied")
                .dimensions(List.of(dimDto))
                .evaluatedAt(LocalDateTime.now())
                .build();

        when(riskService.evaluateBidRisk(bidId)).thenReturn(response);

        mockMvc.perform(post("/api/bids/{bidId}/risk/evaluate", bidId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bidId").value(bidId.toString()))
                .andExpect(jsonPath("$.overallRiskLevel").value("LOW"))
                .andExpect(jsonPath("$.overallRiskScore").value(0))
                .andExpect(jsonPath("$.decisionRecommendation").value("RECOMMEND"))
                .andExpect(jsonPath("$.hardFail").value(false))
                .andExpect(jsonPath("$.dimensions[0].dimension").value("ELIGIBILITY"))
                .andExpect(jsonPath("$.dimensions[0].findings[0].code").value("ELIGIBILITY-000"));
    }

    @Test
    @DisplayName("GET /api/bids/{bidId}/risk returns 200 and latest assessment")
    void testGetBidRiskEndpoint() throws Exception {
        UUID bidId = UUID.randomUUID();

        BidRiskAssessmentResponseDto response = BidRiskAssessmentResponseDto.builder()
                .bidId(bidId)
                .overallRiskLevel(RiskLevel.MEDIUM)
                .overallRiskScore(25)
                .decisionRecommendation(DecisionRecommendation.REVIEW)
                .hardFail(false)
                .summary("Recommendation: REVIEW")
                .reason("Requires review")
                .evaluatedAt(LocalDateTime.now())
                .build();

        when(riskService.getBidRisk(bidId)).thenReturn(response);

        mockMvc.perform(get("/api/bids/{bidId}/risk", bidId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bidId").value(bidId.toString()))
                .andExpect(jsonPath("$.overallRiskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.overallRiskScore").value(25))
                .andExpect(jsonPath("$.decisionRecommendation").value("REVIEW"));
    }

    @Test
    @DisplayName("GET /api/bids/{bidId}/risk/dimensions returns 200 and dimension list")
    void testGetBidRiskDimensionsEndpoint() throws Exception {
        UUID bidId = UUID.randomUUID();

        RiskDimensionResultDto dimDto = RiskDimensionResultDto.builder()
                .dimension(RiskDimension.TEMPORAL)
                .riskLevel(RiskLevel.LOW)
                .riskScore(0)
                .findingCount(0)
                .summary("Temporal valid")
                .build();

        when(riskService.getBidRiskDimensions(bidId)).thenReturn(List.of(dimDto));

        mockMvc.perform(get("/api/bids/{bidId}/risk/dimensions", bidId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dimension").value("TEMPORAL"))
                .andExpect(jsonPath("$[0].riskScore").value(0))
                .andExpect(jsonPath("$[0].riskLevel").value("LOW"));
    }

    @Test
    @DisplayName("GET /api/bids/{bidId}/risk returns 404 when bid is not found")
    void testGetBidRiskNotFound() throws Exception {
        UUID unknownBidId = UUID.randomUUID();
        when(riskService.getBidRisk(unknownBidId))
                .thenThrow(new ResourceNotFoundException("Bid with ID '" + unknownBidId + "' not found"));

        mockMvc.perform(get("/api/bids/{bidId}/risk", unknownBidId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

}
