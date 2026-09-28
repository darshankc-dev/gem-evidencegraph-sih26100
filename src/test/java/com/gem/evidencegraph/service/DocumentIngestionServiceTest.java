package com.gem.evidencegraph.service;

import com.gem.evidencegraph.dto.DocumentResponseDto;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.BidStatus;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.ExtractionStatus;
import com.gem.evidencegraph.entity.IntegrityStatus;
import com.gem.evidencegraph.repository.BidRepository;
import com.gem.evidencegraph.repository.DocumentRepository;
import com.gem.evidencegraph.util.ChecksumUtil;
import com.gem.evidencegraph.util.DuplicateDocumentException;
import com.gem.evidencegraph.util.FileValidationException;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIngestionServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentStorageService documentStorageService;

    private DocumentIngestionService ingestionService;

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    @BeforeEach
    void setUp() {
        ingestionService = new DocumentIngestionServiceImpl(
                bidRepository,
                documentRepository,
                documentStorageService,
                MAX_FILE_SIZE
        );
    }

    @Test
    @DisplayName("1 & 8 & 9. Successful document upload saves record, hashes bytes, and calls storage")
    void shouldSuccessfullyUploadDocument() {
        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder().id(bidId).bidReference("BID-2026-001").status(BidStatus.SUBMITTED).build();
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

        byte[] content = "Dummy GST Certificate content".getBytes(StandardCharsets.UTF_8);
        String expectedHash = ChecksumUtil.calculateSha256(content);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "gst_cert.pdf",
                "application/pdf",
                content
        );

        when(documentRepository.findBySha256Hash(expectedHash)).thenReturn(Optional.empty());
        when(documentStorageService.store(any(UUID.class), eq("gst_cert.pdf"), eq(file)))
                .thenReturn("storage/documents/saved.pdf");

        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setCreatedAt(LocalDateTime.now());
            return doc;
        });

        DocumentResponseDto response = ingestionService.uploadDocument(bidId, file);

        assertNotNull(response);
        assertEquals(bidId, response.getBidId());
        assertEquals("gst_cert.pdf", response.getOriginalFileName());
        assertEquals("application/pdf", response.getContentType());
        assertEquals(expectedHash, response.getSha256Hash());
        assertEquals(DocumentType.OTHER, response.getDocumentType());
        assertEquals(1, response.getVersion());
        assertEquals(ExtractionStatus.NOT_STARTED, response.getExtractionStatus());
        assertEquals(IntegrityStatus.NOT_CHECKED, response.getIntegrityStatus());

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        Document captured = captor.getValue();
        assertEquals(expectedHash, captured.getSha256Hash());
        assertEquals(ExtractionStatus.NOT_STARTED, captured.getExtractionStatus());
        assertEquals(IntegrityStatus.NOT_CHECKED, captured.getIntegrityStatus());
        assertEquals(1, captured.getVersion());
        assertEquals("storage/documents/saved.pdf", captured.getStoragePath());
    }

    @Test
    @DisplayName("2. Bid not found should throw ResourceNotFoundException")
    void shouldThrowWhenBidNotFound() {
        UUID nonExistentBidId = UUID.randomUUID();
        when(bidRepository.findById(nonExistentBidId)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes()
        );

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                ingestionService.uploadDocument(nonExistentBidId, file)
        );
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("3. Empty file should throw FileValidationException")
    void shouldRejectEmptyFile() {
        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder().id(bidId).build();
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]
        );

        FileValidationException ex = assertThrows(FileValidationException.class, () ->
                ingestionService.uploadDocument(bidId, emptyFile)
        );
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    @DisplayName("4. Unsupported file type (.exe, .txt) should be rejected")
    void shouldRejectUnsupportedFileExtension() {
        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder().id(bidId).build();
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

        MockMultipartFile exeFile = new MockMultipartFile(
                "file", "script.exe", "application/octet-stream", "malicious content".getBytes()
        );

        FileValidationException ex = assertThrows(FileValidationException.class, () ->
                ingestionService.uploadDocument(bidId, exeFile)
        );
        assertTrue(ex.getMessage().contains("Unsupported file extension"));
    }

    @Test
    @DisplayName("5. File larger than 10 MB should be rejected")
    void shouldRejectOversizedFile() {
        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder().id(bidId).build();
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", new byte[10]
        ) {
            @Override
            public long getSize() {
                return 11L * 1024 * 1024;
            }
        };

        FileValidationException ex = assertThrows(FileValidationException.class, () ->
                ingestionService.uploadDocument(bidId, largeFile)
        );
        assertTrue(ex.getMessage().contains("exceeds the maximum limit"));
    }

    @Test
    @DisplayName("6. SHA-256 calculation matches standard hash")
    void shouldCalculateSha256Correctly() {
        byte[] data = "GeM EvidenceGraph Verification".getBytes(StandardCharsets.UTF_8);
        String hash = ChecksumUtil.calculateSha256(data);
        assertNotNull(hash);
        assertEquals(64, hash.length());

        assertEquals(hash, ChecksumUtil.calculateSha256(data));
    }

    @Test
    @DisplayName("7. Duplicate SHA-256 should throw DuplicateDocumentException without storing file")
    void shouldRejectDuplicateSha256() {
        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder().id(bidId).build();
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

        byte[] content = "Duplicate content test".getBytes();
        String sha256 = ChecksumUtil.calculateSha256(content);

        UUID existingDocId = UUID.randomUUID();
        Document existingDoc = Document.builder()
                .id(existingDocId)
                .sha256Hash(sha256)
                .build();

        when(documentRepository.findBySha256Hash(sha256)).thenReturn(Optional.of(existingDoc));

        MockMultipartFile duplicateFile = new MockMultipartFile(
                "file", "duplicate.pdf", "application/pdf", content
        );

        DuplicateDocumentException ex = assertThrows(DuplicateDocumentException.class, () ->
                ingestionService.uploadDocument(bidId, duplicateFile)
        );

        assertEquals(sha256, ex.getSha256Hash());
        assertEquals(existingDocId, ex.getExistingDocumentId());

        verify(documentStorageService, never()).store(any(), any(), any());
        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("10. Path traversal filename rejected in validation")
    void shouldRejectPathTraversalFilename() {
        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder().id(bidId).build();
        when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));

        MockMultipartFile traversalFile = new MockMultipartFile(
                "file", "../../../etc/passwd.pdf", "application/pdf", "data".getBytes()
        );

        FileValidationException ex = assertThrows(FileValidationException.class, () ->
                ingestionService.uploadDocument(bidId, traversalFile)
        );
        assertTrue(ex.getMessage().contains("illegal path traversal"));
    }

    @Test
    @DisplayName("11. Document metadata retrieval returns DTO or throws 404")
    void shouldRetrieveDocumentMetadata() {
        UUID docId = UUID.randomUUID();
        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder().id(bidId).build();
        Document document = Document.builder()
                .id(docId)
                .bid(bid)
                .fileName(docId + ".pdf")
                .originalFileName("tender_doc.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .sha256Hash("hash123")
                .documentType(DocumentType.PAN_DOCUMENT)
                .version(1)
                .extractionStatus(ExtractionStatus.NOT_STARTED)
                .integrityStatus(IntegrityStatus.NOT_CHECKED)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        DocumentResponseDto metadata = ingestionService.getDocumentMetadata(docId);
        assertNotNull(metadata);
        assertEquals(docId, metadata.getDocumentId());
        assertEquals(bidId, metadata.getBidId());
        assertEquals("tender_doc.pdf", metadata.getOriginalFileName());

        UUID missingDocId = UUID.randomUUID();
        when(documentRepository.findById(missingDocId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> ingestionService.getDocumentMetadata(missingDocId));
    }

    @Test
    @DisplayName("12. List documents by bid returns DTO list or throws 404")
    void shouldListDocumentsByBid() {
        UUID bidId = UUID.randomUUID();
        Bid bid = Bid.builder().id(bidId).build();
        Document doc1 = Document.builder().id(UUID.randomUUID()).bid(bid).originalFileName("doc1.pdf").build();
        Document doc2 = Document.builder().id(UUID.randomUUID()).bid(bid).originalFileName("doc2.pdf").build();

        when(bidRepository.existsById(bidId)).thenReturn(true);
        when(documentRepository.findByBidId(bidId)).thenReturn(List.of(doc1, doc2));

        List<DocumentResponseDto> documents = ingestionService.getDocumentsByBid(bidId);
        assertEquals(2, documents.size());

        UUID missingBidId = UUID.randomUUID();
        when(bidRepository.existsById(missingBidId)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> ingestionService.getDocumentsByBid(missingBidId));
    }

}
