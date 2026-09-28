package com.gem.evidencegraph.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gem.evidencegraph.compliance.ComplianceController;
import com.gem.evidencegraph.compliance.ComplianceService;
import com.gem.evidencegraph.compliance.ComplianceStatus;
import com.gem.evidencegraph.compliance.OverallComplianceStatus;
import com.gem.evidencegraph.compliance.dto.BidComplianceResultDto;
import com.gem.evidencegraph.compliance.dto.ComplianceEvaluationResultDto;
import com.gem.evidencegraph.compliance.dto.CreateRequirementRequestDto;
import com.gem.evidencegraph.compliance.dto.RequirementResponseDto;
import com.gem.evidencegraph.config.GlobalExceptionHandler;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.RequirementType;
import com.gem.evidencegraph.entity.SourceType;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComplianceController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ComplianceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ComplianceService complianceService;

    @Test
    @DisplayName("Scenario 1: POST /api/tenders/{tenderId}/requirements creates requirement successfully")
    void createRequirement_success() throws Exception {
        UUID tenderId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();

        CreateRequirementRequestDto request = CreateRequirementRequestDto.builder()
                .requirementCode("REQ-GST-01")
                .name("Valid GSTIN Required")
                .description("Must have active GSTIN")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(true)
                .expectedSourceType(SourceType.GST)
                .build();

        RequirementResponseDto responseDto = RequirementResponseDto.builder()
                .id(reqId)
                .tenderId(tenderId)
                .requirementCode("REQ-GST-01")
                .name("Valid GSTIN Required")
                .description("Must have active GSTIN")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(true)
                .expectedSourceType(SourceType.GST)
                .createdAt(LocalDateTime.now())
                .build();

        when(complianceService.createRequirement(eq(tenderId), any(CreateRequirementRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/tenders/{tenderId}/requirements", tenderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(reqId.toString()))
                .andExpect(jsonPath("$.tenderId").value(tenderId.toString()))
                .andExpect(jsonPath("$.requirementCode").value("REQ-GST-01"))
                .andExpect(jsonPath("$.name").value("Valid GSTIN Required"))
                .andExpect(jsonPath("$.mandatory").value(true));
    }

    @Test
    @DisplayName("Scenario 2: GET /api/tenders/{tenderId}/requirements retrieves tender requirements")
    void getRequirementsByTender_success() throws Exception {
        UUID tenderId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();

        RequirementResponseDto responseDto = RequirementResponseDto.builder()
                .id(reqId)
                .tenderId(tenderId)
                .requirementCode("REQ-GST-01")
                .name("Valid GSTIN Required")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(true)
                .build();

        when(complianceService.getRequirementsByTender(tenderId)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/tenders/{tenderId}/requirements", tenderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reqId.toString()))
                .andExpect(jsonPath("$[0].requirementCode").value("REQ-GST-01"));
    }

    @Test
    @DisplayName("Scenario 3: POST /api/tenders/{tenderId}/requirements rejects missing tender (404)")
    void createRequirement_tenderNotFound() throws Exception {
        UUID tenderId = UUID.randomUUID();

        CreateRequirementRequestDto request = CreateRequirementRequestDto.builder()
                .requirementCode("REQ-GST-01")
                .name("Valid GSTIN Required")
                .requirementType(RequirementType.REGISTRATION)
                .mandatory(true)
                .build();

        when(complianceService.createRequirement(eq(tenderId), any(CreateRequirementRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Tender not found: " + tenderId));

        mockMvc.perform(post("/api/tenders/{tenderId}/requirements", tenderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 4: POST /api/tenders/{tenderId}/requirements rejects invalid requirement data (400)")
    void createRequirement_invalidData() throws Exception {
        UUID tenderId = UUID.randomUUID();

        CreateRequirementRequestDto invalidRequest = CreateRequirementRequestDto.builder()
                .requirementCode("")
                .name("")
                .requirementType(null)
                .mandatory(true)
                .build();

        mockMvc.perform(post("/api/tenders/{tenderId}/requirements", tenderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/requirements/{requirementId} returns requirement by id")
    void getRequirementById_success() throws Exception {
        UUID reqId = UUID.randomUUID();

        RequirementResponseDto responseDto = RequirementResponseDto.builder()
                .id(reqId)
                .requirementCode("REQ-OEM")
                .name("OEM Authorization")
                .requirementType(RequirementType.DOCUMENT)
                .mandatory(true)
                .expectedDocumentType(DocumentType.OEM_AUTHORIZATION)
                .build();

        when(complianceService.getRequirementById(reqId)).thenReturn(responseDto);

        mockMvc.perform(get("/api/requirements/{requirementId}", reqId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reqId.toString()))
                .andExpect(jsonPath("$.requirementCode").value("REQ-OEM"));
    }

    @Test
    @DisplayName("POST /api/bids/{bidId}/compliance/evaluate evaluates bid compliance successfully")
    void evaluateBidCompliance_success() throws Exception {
        UUID bidId = UUID.randomUUID();
        UUID tenderId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();

        ComplianceEvaluationResultDto evalResult = ComplianceEvaluationResultDto.builder()
                .requirementId(reqId)
                .requirementCode("REQ-GST")
                .requirementName("Valid GST")
                .mandatory(true)
                .status(ComplianceStatus.COMPLIANT)
                .confidence(1.0)
                .reason("GST verified")
                .build();

        BidComplianceResultDto resultDto = BidComplianceResultDto.builder()
                .bidId(bidId)
                .tenderId(tenderId)
                .bidderId(bidderId)
                .overallStatus(OverallComplianceStatus.COMPLIANT)
                .compliancePercentage(100.0)
                .mandatoryRequirementCount(1)
                .compliantCount(1)
                .nonCompliantCount(0)
                .missingCount(0)
                .unverifiedCount(0)
                .contradictoryCount(0)
                .pendingReviewCount(0)
                .requirementResults(List.of(evalResult))
                .summary("All 1 evaluated requirements are compliant.")
                .evaluatedAt(LocalDateTime.now())
                .build();

        when(complianceService.evaluateBidCompliance(bidId)).thenReturn(resultDto);

        mockMvc.perform(post("/api/bids/{bidId}/compliance/evaluate", bidId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bidId").value(bidId.toString()))
                .andExpect(jsonPath("$.overallStatus").value("COMPLIANT"))
                .andExpect(jsonPath("$.compliancePercentage").value(100.0))
                .andExpect(jsonPath("$.compliantCount").value(1))
                .andExpect(jsonPath("$.requirementResults[0].requirementCode").value("REQ-GST"));
    }

    @Test
    @DisplayName("GET /api/bids/{bidId}/compliance retrieves latest bid compliance result")
    void getBidCompliance_success() throws Exception {
        UUID bidId = UUID.randomUUID();
        UUID tenderId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();

        BidComplianceResultDto resultDto = BidComplianceResultDto.builder()
                .bidId(bidId)
                .tenderId(tenderId)
                .bidderId(bidderId)
                .overallStatus(OverallComplianceStatus.REVIEW)
                .compliancePercentage(50.0)
                .mandatoryRequirementCount(2)
                .compliantCount(1)
                .missingCount(1)
                .summary("Compliance requires officer review. 1 mandatory requirement(s) are missing or unresolved.")
                .evaluatedAt(LocalDateTime.now())
                .build();

        when(complianceService.getLatestBidCompliance(bidId)).thenReturn(resultDto);

        mockMvc.perform(get("/api/bids/{bidId}/compliance", bidId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bidId").value(bidId.toString()))
                .andExpect(jsonPath("$.overallStatus").value("REVIEW"))
                .andExpect(jsonPath("$.compliancePercentage").value(50.0));
    }
}
