package com.gem.evidencegraph.extraction;

import com.gem.evidencegraph.dto.ExtractionResponseDto;
import com.gem.evidencegraph.entity.Bid;
import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.Document;
import com.gem.evidencegraph.entity.DocumentType;
import com.gem.evidencegraph.entity.ExtractionMethod;
import com.gem.evidencegraph.entity.ExtractionStatus;
import com.gem.evidencegraph.entity.IntegrityStatus;
import com.gem.evidencegraph.repository.ClaimRepository;
import com.gem.evidencegraph.repository.DocumentRepository;
import com.gem.evidencegraph.util.FileStorageException;
import com.gem.evidencegraph.util.FileValidationException;
import com.gem.evidencegraph.util.ResourceNotFoundException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentExtractionTest {

    @TempDir
    Path tempDir;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ClaimRepository claimRepository;

    private DocumentTextExtractionService textExtractionService;
    private ClaimExtractionService claimExtractionService;
    private DocumentExtractionPipelineService pipelineService;

    @BeforeEach
    void setUp() {
        textExtractionService = new PdfTextExtractionService();
        claimExtractionService = new ClaimExtractionServiceImpl();
        pipelineService = new DocumentExtractionPipelineServiceImpl(
                documentRepository,
                claimRepository,
                textExtractionService,
                claimExtractionService
        );
    }

    private Path createTestPdf(String text) throws IOException {
        Path pdfPath = tempDir.resolve("test_" + UUID.randomUUID() + ".pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                for (String line : text.split("\n")) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -15);
                }
                contentStream.endText();
            }
            document.save(pdfPath.toFile());
        }
        return pdfPath;
    }

    private Path createEmptyTestPdf() throws IOException {
        Path pdfPath = tempDir.resolve("empty_" + UUID.randomUUID() + ".pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdfPath.toFile());
        }
        return pdfPath;
    }

    @Test
    @DisplayName("1. PDF text extraction reads available text layer accurately")
    void shouldExtractTextFromPdf() throws IOException {
        Path pdfPath = createTestPdf("Government of India\nProcurement Portal\nGSTIN: 29ABCDE1234F1Z5");
        DocumentTextResult result = textExtractionService.extractText(pdfPath);

        assertNotNull(result);
        assertTrue(result.isHasTextLayer());
        assertEquals(1, result.getPageCount());
        assertTrue(result.getFullText().contains("GSTIN: 29ABCDE1234F1Z5"));
        assertEquals(result.getFullText(), result.getPageTexts().get(1));
    }

    @Test
    @DisplayName("2. Empty/no-text PDF marks hasTextLayer as false")
    void shouldHandleEmptyPdf() throws IOException {
        Path emptyPdf = createEmptyTestPdf();
        DocumentTextResult result = textExtractionService.extractText(emptyPdf);

        assertNotNull(result);
        assertFalse(result.isHasTextLayer());
        assertEquals(0, result.getTotalCharacterCount());
    }

    @Test
    @DisplayName("3. Malformed PDF handling throws FileValidationException")
    void shouldHandleMalformedPdf() throws IOException {
        Path malformedPath = tempDir.resolve("corrupt.pdf");
        Files.write(malformedPath, "Not a PDF content at all".getBytes(StandardCharsets.UTF_8));

        assertThrows(FileValidationException.class, () -> textExtractionService.extractText(malformedPath));
    }

    @Test
    @DisplayName("4 & 5 & 6. GSTIN, PAN, and Udyam regex pattern detection")
    void shouldDetectProcurementIdentifiers() throws IOException {
        String content = """
                GST Certificate of Registration
                GSTIN: 29ABCDE1234F1Z5
                Permanent Account Number PAN: ABCDE1234F
                Udyam Registration Number: UDYAM-KR-03-0012345
                """;
        Path pdfPath = createTestPdf(content);
        DocumentTextResult textResult = textExtractionService.extractText(pdfPath);

        Document doc = Document.builder().id(UUID.randomUUID()).build();
        List<Claim> claims = claimExtractionService.extractClaims(doc, textResult);

        assertNotNull(claims);
        assertEquals(3, claims.size());

        assertTrue(claims.stream().anyMatch(c -> "GSTIN".equals(c.getFieldName()) && "29ABCDE1234F1Z5".equals(c.getNormalizedValue())));
        assertTrue(claims.stream().anyMatch(c -> "PAN".equals(c.getFieldName()) && "ABCDE1234F".equals(c.getNormalizedValue())));
        assertTrue(claims.stream().anyMatch(c -> "UDYAM_NUMBER".equals(c.getFieldName()) && "UDYAM-KR-03-0012345".equals(c.getNormalizedValue())));
    }

    @Test
    @DisplayName("7 & 8 & 9. Legal Name, Address, and Date extraction")
    void shouldExtractMetadataFields() throws IOException {
        String content = """
                Legal Name: Alpha Infotech Private Limited
                Registered Address: Plot 100 Electronics City Phase 1 Bengaluru
                Registration Date: 15/08/2021
                Expiry Date: 31/12/2030
                Certificate No: CERT-2021-998877
                """;
        Path pdfPath = createTestPdf(content);
        DocumentTextResult textResult = textExtractionService.extractText(pdfPath);

        Document doc = Document.builder().id(UUID.randomUUID()).build();
        List<Claim> claims = claimExtractionService.extractClaims(doc, textResult);

        assertTrue(claims.stream().anyMatch(c -> "LEGAL_NAME".equals(c.getFieldName()) && c.getNormalizedValue().contains("ALPHA INFOTECH PRIVATE LIMITED")));
        assertTrue(claims.stream().anyMatch(c -> "ADDRESS".equals(c.getFieldName()) && c.getNormalizedValue().contains("BENGALURU")));
        assertTrue(claims.stream().anyMatch(c -> "REGISTRATION_DATE".equals(c.getFieldName()) && "2021-08-15".equals(c.getNormalizedValue())));
        assertTrue(claims.stream().anyMatch(c -> "EXPIRY_DATE".equals(c.getFieldName()) && "2030-12-31".equals(c.getNormalizedValue())));
        assertTrue(claims.stream().anyMatch(c -> "CERTIFICATE_NUMBER".equals(c.getFieldName()) && "CERT-2021-998877".equals(c.getNormalizedValue())));
    }

    @Test
    @DisplayName("10. Normalization utility rules")
    void shouldNormalizeFieldsCorrectly() {
        assertEquals("29ABCDE1234F1Z5", ClaimNormalizationUtil.normalize("GSTIN", " 29abcde1234f1z5 "));
        assertEquals("ABCDE1234F", ClaimNormalizationUtil.normalize("PAN", " abcde1234f "));
        assertEquals("UDYAM-KR-03-0012345", ClaimNormalizationUtil.normalize("UDYAM_NUMBER", "udyam  kr  03  0012345"));
        assertEquals("ALPHA INFOTECH PVT LTD", ClaimNormalizationUtil.normalize("LEGAL_NAME", "  Alpha   Infotech  Pvt   Ltd.  "));
        assertEquals("100 MAIN STREET BENGALURU", ClaimNormalizationUtil.normalize("ADDRESS", "100 main street\nbengaluru\n"));
        assertEquals("2021-08-15", ClaimNormalizationUtil.normalize("REGISTRATION_DATE", "15/08/2021"));
    }

    @Test
    @DisplayName("11. Confidence assignment (unambiguous vs conflicting)")
    void shouldAssignConfidenceScores() {
        DocumentTextResult singleGstinResult = DocumentTextResult.builder()
                .hasTextLayer(true)
                .fullText("GSTIN: 29ABCDE1234F1Z5")
                .pageTexts(java.util.Map.of(1, "GSTIN: 29ABCDE1234F1Z5"))
                .build();

        Document doc = Document.builder().id(UUID.randomUUID()).build();
        List<Claim> singleClaims = claimExtractionService.extractClaims(doc, singleGstinResult);
        assertEquals(1, singleClaims.size());
        assertEquals(0.95, singleClaims.get(0).getConfidence());

        DocumentTextResult conflictingResult = DocumentTextResult.builder()
                .hasTextLayer(true)
                .fullText("First GSTIN: 29ABCDE1234F1Z5\nSecond GSTIN: 27XYZAB9876C1Z9")
                .pageTexts(java.util.Map.of(1, "First GSTIN: 29ABCDE1234F1Z5\nSecond GSTIN: 27XYZAB9876C1Z9"))
                .build();

        List<Claim> conflictingClaims = claimExtractionService.extractClaims(doc, conflictingResult);
        assertEquals(2, conflictingClaims.size());
        assertEquals(0.75, conflictingClaims.get(0).getConfidence());
        assertEquals(0.75, conflictingClaims.get(1).getConfidence());
    }

    @Test
    @DisplayName("12 & 13 & 14. Full pipeline execution: Document transitions NOT_STARTED -> PROCESSING -> EXTRACTED, replaces old claims")
    void shouldExecuteFullExtractionPipeline() throws IOException {
        Path pdfPath = createTestPdf("Company GSTIN: 29ABCDE1234F1Z5\nPAN: ABCDE1234F");

        UUID docId = UUID.randomUUID();
        Document document = Document.builder()
                .id(docId)
                .storagePath(pdfPath.toString())
                .extractionStatus(ExtractionStatus.NOT_STARTED)
                .integrityStatus(IntegrityStatus.NOT_CHECKED)
                .version(1)
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));
        when(claimRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ExtractionResponseDto response = pipelineService.executeExtraction(docId);

        assertNotNull(response);
        assertEquals(ExtractionStatus.EXTRACTED, response.getExtractionStatus());
        assertEquals(2, response.getNumberOfClaims());
        assertEquals(2, response.getClaims().size());

        verify(claimRepository).deleteByDocumentId(docId);

        assertEquals(ExtractionStatus.EXTRACTED, document.getExtractionStatus());
        verify(documentRepository, org.mockito.Mockito.atLeast(2)).save(document);
    }

    @Test
    @DisplayName("14b. Empty PDF sets status to LOW_CONFIDENCE")
    void shouldSetLowConfidenceForEmptyPdf() throws IOException {
        Path emptyPdf = createEmptyTestPdf();
        UUID docId = UUID.randomUUID();
        Document document = Document.builder()
                .id(docId)
                .storagePath(emptyPdf.toString())
                .extractionStatus(ExtractionStatus.NOT_STARTED)
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        ExtractionResponseDto response = pipelineService.executeExtraction(docId);

        assertEquals(ExtractionStatus.LOW_CONFIDENCE, response.getExtractionStatus());
        assertEquals(0, response.getNumberOfClaims());
        assertEquals(ExtractionStatus.LOW_CONFIDENCE, document.getExtractionStatus());
    }

    @Test
    @DisplayName("15. Missing document throws ResourceNotFoundException")
    void shouldThrowWhenDocumentNotFound() {
        UUID missingId = UUID.randomUUID();
        when(documentRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> pipelineService.executeExtraction(missingId));
    }

    @Test
    @DisplayName("16. Missing storage file sets FAILED status and throws FileStorageException")
    void shouldHandleMissingStorageFile() {
        UUID docId = UUID.randomUUID();
        Document document = Document.builder()
                .id(docId)
                .storagePath("non_existent_folder/missing.pdf")
                .extractionStatus(ExtractionStatus.NOT_STARTED)
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        assertThrows(FileStorageException.class, () -> pipelineService.executeExtraction(docId));
        assertEquals(ExtractionStatus.FAILED, document.getExtractionStatus());
    }

}
