package com.gem.evidencegraph.controller;

import com.gem.evidencegraph.dto.ClaimResponseDto;
import com.gem.evidencegraph.dto.DocumentResponseDto;
import com.gem.evidencegraph.dto.ExtractionResponseDto;
import com.gem.evidencegraph.extraction.DocumentExtractionPipelineService;
import com.gem.evidencegraph.service.DocumentIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;
    private final DocumentExtractionPipelineService extractionPipelineService;

    public DocumentController(
            DocumentIngestionService documentIngestionService,
            DocumentExtractionPipelineService extractionPipelineService) {
        this.documentIngestionService = documentIngestionService;
        this.extractionPipelineService = extractionPipelineService;
    }

    @PostMapping(value = "/bids/{bidId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponseDto> uploadDocument(
            @PathVariable UUID bidId,
            @RequestParam("file") MultipartFile file) {
        DocumentResponseDto response = documentIngestionService.uploadDocument(bidId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentResponseDto> getDocumentMetadata(@PathVariable UUID documentId) {
        DocumentResponseDto response = documentIngestionService.getDocumentMetadata(documentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bids/{bidId}/documents")
    public ResponseEntity<List<DocumentResponseDto>> getDocumentsByBid(@PathVariable UUID bidId) {
        List<DocumentResponseDto> documents = documentIngestionService.getDocumentsByBid(bidId);
        return ResponseEntity.ok(documents);
    }

    @PostMapping("/documents/{documentId}/extract")
    public ResponseEntity<ExtractionResponseDto> extractClaims(@PathVariable UUID documentId) {
        ExtractionResponseDto response = extractionPipelineService.executeExtraction(documentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/documents/{documentId}/claims")
    public ResponseEntity<List<ClaimResponseDto>> getClaims(@PathVariable UUID documentId) {
        List<ClaimResponseDto> claims = extractionPipelineService.getClaimsByDocument(documentId);
        return ResponseEntity.ok(claims);
    }

}
