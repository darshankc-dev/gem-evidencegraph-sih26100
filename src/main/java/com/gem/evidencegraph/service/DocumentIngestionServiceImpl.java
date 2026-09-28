package com.gem.evidencegraph.service;

import com.gem.evidencegraph.dto.DocumentResponseDto;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.ExtractionStatus;
import com.gem.evidencegraph.entity.IntegrityStatus;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.DocumentRepository;
import com.gem.evidencegraph.util.ChecksumUtil;
import com.gem.evidencegraph.util.DuplicateDocumentException;
import com.gem.evidencegraph.util.FileStorageException;
import com.gem.evidencegraph.util.FileValidationException;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

    private static final long DEFAULT_MAX_SIZE = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".png", ".jpg", ".jpeg");

    private static final Map<String, Set<String>> EXTENSION_TO_MIMES = Map.of(
            ".pdf", Set.of("application/pdf"),
            ".png", Set.of("image/png"),
            ".jpg", Set.of("image/jpeg", "image/pjpeg"),
            ".jpeg", Set.of("image/jpeg", "image/pjpeg")
    );

    private final BidRepository bidRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;
    private final long maxFileSizeBytes;

    public DocumentIngestionServiceImpl(
            BidRepository bidRepository,
            DocumentRepository documentRepository,
            DocumentStorageService documentStorageService,
            @Value("${app.storage.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        this.bidRepository = bidRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
        this.maxFileSizeBytes = maxFileSizeBytes > 0 ? maxFileSizeBytes : DEFAULT_MAX_SIZE;
    }

    @Override
    @Transactional
    public DocumentResponseDto uploadDocument(UUID bidId, MultipartFile file) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResourceNotFoundException("Bid with ID '" + bidId + "' not found"));

        validateFile(file);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new FileStorageException("Unable to read uploaded file contents", e);
        }

        String sha256Hash = ChecksumUtil.calculateSha256(fileBytes);

        Optional<Document> existingDocument = documentRepository.findBySha256Hash(sha256Hash);
        if (existingDocument.isPresent()) {
            throw new DuplicateDocumentException(
                    "Duplicate document detected: SHA-256 hash '" + sha256Hash + "' already exists in the system",
                    sha256Hash,
                    existingDocument.get().getId()
            );
        }

        UUID documentId = UUID.randomUUID();
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");
        String extension = getFileExtension(originalFilename);
        String serverFileName = documentId + extension;

        String storagePath = documentStorageService.store(documentId, originalFilename, file);

        Document document = Document.builder()
                .id(documentId)
                .bid(bid)
                .fileName(serverFileName)
                .originalFileName(originalFilename)
                .storagePath(storagePath)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .sha256Hash(sha256Hash)
                .documentType(DocumentType.OTHER)
                .version(1)
                .uploadedAt(LocalDateTime.now())
                .extractionStatus(ExtractionStatus.NOT_STARTED)
                .integrityStatus(IntegrityStatus.NOT_CHECKED)
                .build();

        Document savedDocument = documentRepository.save(document);
        return DocumentResponseDto.fromEntity(savedDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponseDto getDocumentMetadata(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document with ID '" + documentId + "' not found"));
        return DocumentResponseDto.fromEntity(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDto> getDocumentsByBid(UUID bidId) {
        if (!bidRepository.existsById(bidId)) {
            throw new ResourceNotFoundException("Bid with ID '" + bidId + "' not found");
        }
        return documentRepository.findByBidId(bidId)
                .stream()
                .map(DocumentResponseDto::fromEntity)
                .toList();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("Uploaded file is missing or empty");
        }

        if (file.getSize() > this.maxFileSizeBytes) {
            throw new FileValidationException("File size exceeds the maximum limit of "
                    + (this.maxFileSizeBytes / (1024 * 1024)) + " MB (actual: " + file.getSize() + " bytes)");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new FileValidationException("Original filename cannot be empty");
        }

        String cleanedPath = StringUtils.cleanPath(originalFilename);
        if (cleanedPath.contains("..") || cleanedPath.contains("/") || cleanedPath.contains("\\")) {
            throw new FileValidationException("Filename contains illegal path traversal characters: " + originalFilename);
        }

        String extension = getFileExtension(cleanedPath);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new FileValidationException("Unsupported file extension: '" + extension + "'. Allowed extensions: " + ALLOWED_EXTENSIONS);
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new FileValidationException("Content-Type header is required");
        }

        Set<String> validMimes = EXTENSION_TO_MIMES.get(extension);
        if (validMimes == null || !validMimes.contains(contentType.toLowerCase())) {
            throw new FileValidationException("MIME type mismatch: Content-Type '" + contentType
                    + "' is not permitted for extension '" + extension + "'");
        }

        try {
            byte[] previewBytes = file.getBytes();
            if (previewBytes.length >= 2 && previewBytes[0] == 'M' && previewBytes[1] == 'Z') {
                throw new FileValidationException("Executable binary files (DOS/PE executable) are not allowed");
            }
            if (previewBytes.length >= 4 && previewBytes[0] == 0x7F && previewBytes[1] == 'E' && previewBytes[2] == 'L' && previewBytes[3] == 'F') {
                throw new FileValidationException("Executable ELF binaries are not allowed");
            }
        } catch (IOException e) {
            throw new FileStorageException("Unable to inspect file header", e);
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex).toLowerCase();
        }
        return "";
    }

}
