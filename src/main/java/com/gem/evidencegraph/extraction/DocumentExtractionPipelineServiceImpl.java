package com.gem.evidencegraph.extraction;

import com.gem.evidencegraph.dto.ClaimResponseDto;
import com.gem.evidencegraph.dto.ExtractionResponseDto;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.ExtractionStatus;
import com.gem.evidencegraph.repository.ClaimRepository;
import com.gem.evidencegraph.repository.DocumentRepository;
import com.gem.evidencegraph.util.FileStorageException;
import com.gem.evidencegraph.util.FileValidationException;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentExtractionPipelineServiceImpl implements DocumentExtractionPipelineService {

    private final DocumentRepository documentRepository;
    private final ClaimRepository claimRepository;
    private final DocumentTextExtractionService textExtractionService;
    private final ClaimExtractionService claimExtractionService;

    public DocumentExtractionPipelineServiceImpl(
            DocumentRepository documentRepository,
            ClaimRepository claimRepository,
            DocumentTextExtractionService textExtractionService,
            ClaimExtractionService claimExtractionService) {
        this.documentRepository = documentRepository;
        this.claimRepository = claimRepository;
        this.textExtractionService = textExtractionService;
        this.claimExtractionService = claimExtractionService;
    }

    @Override
    @Transactional
    public ExtractionResponseDto executeExtraction(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document with ID '" + documentId + "' not found"));

        if (document.getStoragePath() == null || document.getStoragePath().isBlank()) {
            document.setExtractionStatus(ExtractionStatus.FAILED);
            documentRepository.save(document);
            throw new FileStorageException("Document record does not contain a valid storagePath");
        }

        Path filePath = Paths.get(document.getStoragePath()).normalize();
        if (!Files.exists(filePath)) {
            document.setExtractionStatus(ExtractionStatus.FAILED);
            documentRepository.save(document);
            throw new FileStorageException("Physical document file missing at: " + document.getStoragePath());
        }

        document.setExtractionStatus(ExtractionStatus.PROCESSING);
        documentRepository.save(document);

        DocumentTextResult textResult;
        try {
            textResult = textExtractionService.extractText(filePath);
        } catch (FileValidationException | FileStorageException ex) {
            document.setExtractionStatus(ExtractionStatus.FAILED);
            documentRepository.save(document);
            throw ex;
        } catch (Exception ex) {
            document.setExtractionStatus(ExtractionStatus.FAILED);
            documentRepository.save(document);
            throw new FileValidationException("Failed to extract text from document: " + ex.getMessage());
        }

        if (!textResult.isHasTextLayer() || textResult.getFullText().isBlank()) {
            claimRepository.deleteByDocumentId(documentId);
            document.setExtractionStatus(ExtractionStatus.LOW_CONFIDENCE);
            documentRepository.save(document);

            return ExtractionResponseDto.builder()
                    .documentId(documentId)
                    .extractionStatus(ExtractionStatus.LOW_CONFIDENCE)
                    .extractedTextLength(0)
                    .numberOfClaims(0)
                    .claims(Collections.emptyList())
                    .build();
        }

        List<Claim> detectedClaims = claimExtractionService.extractClaims(document, textResult);

        claimRepository.deleteByDocumentId(documentId);

        if (detectedClaims.isEmpty()) {
            document.setExtractionStatus(ExtractionStatus.LOW_CONFIDENCE);
            documentRepository.save(document);

            return ExtractionResponseDto.builder()
                    .documentId(documentId)
                    .extractionStatus(ExtractionStatus.LOW_CONFIDENCE)
                    .extractedTextLength(textResult.getTotalCharacterCount())
                    .numberOfClaims(0)
                    .claims(Collections.emptyList())
                    .build();
        }

        List<Claim> savedClaims = claimRepository.saveAll(detectedClaims);
        document.setExtractionStatus(ExtractionStatus.EXTRACTED);
        documentRepository.save(document);

        List<ClaimResponseDto> claimDtos = savedClaims.stream()
                .map(ClaimResponseDto::fromEntity)
                .toList();

        return ExtractionResponseDto.builder()
                .documentId(documentId)
                .extractionStatus(ExtractionStatus.EXTRACTED)
                .extractedTextLength(textResult.getTotalCharacterCount())
                .numberOfClaims(savedClaims.size())
                .claims(claimDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaimResponseDto> getClaimsByDocument(UUID documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Document with ID '" + documentId + "' not found");
        }
        return claimRepository.findByDocumentId(documentId)
                .stream()
                .map(ClaimResponseDto::fromEntity)
                .toList();
    }

}
