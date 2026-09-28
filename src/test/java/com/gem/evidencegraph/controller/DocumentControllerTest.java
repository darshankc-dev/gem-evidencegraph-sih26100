package com.gem.evidencegraph.controller;

import com.gem.evidencegraph.config.GlobalExceptionHandler;
import com.gem.evidencegraph.dto.ClaimResponseDto;
import com.gem.evidencegraph.dto.DocumentResponseDto;
import com.gem.evidencegraph.dto.ExtractionResponseDto;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.ExtractionMethod;
import com.gem.evidencegraph.entity.ExtractionStatus;
import com.gem.evidencegraph.entity.IntegrityStatus;
import com.gem.evidencegraph.extraction.DocumentExtractionPipelineService;
import com.gem.evidencegraph.security.SecurityConfig;
import com.gem.evidencegraph.service.DocumentIngestionService;
import com.gem.evidencegraph.util.DuplicateDocumentException;
import com.gem.evidencegraph.util.FileValidationException;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentIngestionService documentIngestionService;

    @MockBean
    private DocumentExtractionPipelineService extractionPipelineService;

    @Test
    @DisplayName("POST /api/bids/{bidId}/documents should return 201 Created on valid upload")
    void shouldUploadDocumentSuccessfully() throws Exception {
        UUID bidId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        DocumentResponseDto responseDto = DocumentResponseDto.builder()
                .documentId(documentId)
                .bidId(bidId)
                .fileName(documentId + ".pdf")
                .originalFileName("gst_certificate.pdf")
                .contentType("application/pdf")
                .fileSize(2048L)
                .sha256Hash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .documentType(DocumentType.OTHER)
                .version(1)
                .extractionStatus(ExtractionStatus.NOT_STARTED)
                .integrityStatus(IntegrityStatus.NOT_CHECKED)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(documentIngestionService.uploadDocument(eq(bidId), any())).thenReturn(responseDto);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "gst_certificate.pdf",
                "application/pdf",
                "PDF byte stream".getBytes()
        );

        mockMvc.perform(multipart("/api/bids/{bidId}/documents", bidId).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.bidId").value(bidId.toString()))
                .andExpect(jsonPath("$.originalFileName").value("gst_certificate.pdf"))
                .andExpect(jsonPath("$.extractionStatus").value("NOT_STARTED"))
                .andExpect(jsonPath("$.integrityStatus").value("NOT_CHECKED"));
    }

    @Test
    @DisplayName("POST /api/bids/{bidId}/documents should return 404 when bid not found")
    void shouldReturn404WhenBidNotFound() throws Exception {
        UUID nonExistentBidId = UUID.randomUUID();
        when(documentIngestionService.uploadDocument(eq(nonExistentBidId), any()))
                .thenThrow(new ResourceNotFoundException("Bid with ID '" + nonExistentBidId + "' not found"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "data".getBytes()
        );

        mockMvc.perform(multipart("/api/bids/{bidId}/documents", nonExistentBidId).file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Bid with ID '" + nonExistentBidId + "' not found"));
    }

    @Test
    @DisplayName("POST /api/bids/{bidId}/documents should return 400 on invalid file")
    void shouldReturn400OnInvalidFile() throws Exception {
        UUID bidId = UUID.randomUUID();
        when(documentIngestionService.uploadDocument(eq(bidId), any()))
                .thenThrow(new FileValidationException("Unsupported file extension: '.exe'"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "danger.exe", "application/octet-stream", "data".getBytes()
        );

        mockMvc.perform(multipart("/api/bids/{bidId}/documents", bidId).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unsupported file extension: '.exe'"));
    }

    @Test
    @DisplayName("POST /api/bids/{bidId}/documents should return 409 on duplicate document")
    void shouldReturn409OnDuplicateFile() throws Exception {
        UUID bidId = UUID.randomUUID();
        UUID existingId = UUID.randomUUID();
        String hash = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

        when(documentIngestionService.uploadDocument(eq(bidId), any()))
                .thenThrow(new DuplicateDocumentException("Duplicate document detected", hash, existingId));

        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "data".getBytes()
        );

        mockMvc.perform(multipart("/api/bids/{bidId}/documents", bidId).file(file))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.sha256Hash").value(hash))
                .andExpect(jsonPath("$.existingDocumentId").value(existingId.toString()));
    }

    @Test
    @DisplayName("GET /api/documents/{documentId} should return 200 with document metadata")
    void shouldReturnDocumentMetadata() throws Exception {
        UUID docId = UUID.randomUUID();
        DocumentResponseDto dto = DocumentResponseDto.builder()
                .documentId(docId)
                .originalFileName("udyam.pdf")
                .contentType("application/pdf")
                .sha256Hash("hash123")
                .documentType(DocumentType.UDYAM_CERTIFICATE)
                .build();

        when(documentIngestionService.getDocumentMetadata(docId)).thenReturn(dto);

        mockMvc.perform(get("/api/documents/{documentId}", docId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(docId.toString()))
                .andExpect(jsonPath("$.originalFileName").value("udyam.pdf"));
    }

    @Test
    @DisplayName("GET /api/bids/{bidId}/documents should return 200 with list of documents")
    void shouldReturnDocumentsForBid() throws Exception {
        UUID bidId = UUID.randomUUID();
        DocumentResponseDto doc1 = DocumentResponseDto.builder()
                .documentId(UUID.randomUUID())
                .bidId(bidId)
                .originalFileName("doc1.pdf")
                .build();

        when(documentIngestionService.getDocumentsByBid(bidId)).thenReturn(List.of(doc1));

        mockMvc.perform(get("/api/bids/{bidId}/documents", bidId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bidId").value(bidId.toString()))
                .andExpect(jsonPath("$[0].originalFileName").value("doc1.pdf"));
    }

    @Test
    @DisplayName("POST /api/documents/{documentId}/extract should trigger extraction and return claims")
    void shouldTriggerExtractionSuccessfully() throws Exception {
        UUID docId = UUID.randomUUID();
        ClaimResponseDto claimDto = ClaimResponseDto.builder()
                .id(UUID.randomUUID())
                .documentId(docId)
                .fieldName("GSTIN")
                .fieldValue("29ABCDE1234F1Z5")
                .normalizedValue("29ABCDE1234F1Z5")
                .confidence(0.95)
                .extractionMethod(ExtractionMethod.DETERMINISTIC)
                .sourcePage(1)
                .sourceLocation("Detected via GSTIN format pattern on page 1")
                .build();

        ExtractionResponseDto responseDto = ExtractionResponseDto.builder()
                .documentId(docId)
                .extractionStatus(ExtractionStatus.EXTRACTED)
                .extractedTextLength(500)
                .numberOfClaims(1)
                .claims(List.of(claimDto))
                .build();

        when(extractionPipelineService.executeExtraction(docId)).thenReturn(responseDto);

        mockMvc.perform(post("/api/documents/{documentId}/extract", docId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(docId.toString()))
                .andExpect(jsonPath("$.extractionStatus").value("EXTRACTED"))
                .andExpect(jsonPath("$.numberOfClaims").value(1))
                .andExpect(jsonPath("$.claims[0].fieldName").value("GSTIN"))
                .andExpect(jsonPath("$.claims[0].normalizedValue").value("29ABCDE1234F1Z5"))
                .andExpect(jsonPath("$.claims[0].confidence").value(0.95))
                .andExpect(jsonPath("$.claims[0].extractionMethod").value("DETERMINISTIC"));
    }

    @Test
    @DisplayName("GET /api/documents/{documentId}/claims should return list of claims")
    void shouldReturnClaimsForDocument() throws Exception {
        UUID docId = UUID.randomUUID();
        ClaimResponseDto claimDto = ClaimResponseDto.builder()
                .id(UUID.randomUUID())
                .documentId(docId)
                .fieldName("PAN")
                .fieldValue("ABCDE1234F")
                .normalizedValue("ABCDE1234F")
                .confidence(0.95)
                .extractionMethod(ExtractionMethod.DETERMINISTIC)
                .sourcePage(1)
                .build();

        when(extractionPipelineService.getClaimsByDocument(docId)).thenReturn(List.of(claimDto));

        mockMvc.perform(get("/api/documents/{documentId}/claims", docId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fieldName").value("PAN"))
                .andExpect(jsonPath("$[0].normalizedValue").value("ABCDE1234F"));
    }

}
