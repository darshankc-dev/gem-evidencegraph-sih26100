package com.gem.evidencegraph.demo;

import com.gem.evidencegraph.extraction.DocumentTextResult;
import com.gem.evidencegraph.extraction.PdfTextExtractionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SyntheticPdfGeneratorTest {

    private final PdfTextExtractionService textExtractionService = new PdfTextExtractionService();

    @Test
    @DisplayName("SyntheticPdfGenerator produces valid, readable PDF bytes matching text layer")
    void shouldGenerateReadablePdf() {
        byte[] pdfBytes = SyntheticPdfGenerator.generatePdf(
                "GOODS AND SERVICES TAX REGISTRATION CERTIFICATE",
                List.of(
                        "Legal Name: Apex Secure Systems Pvt Ltd",
                        "GSTIN: 29ABCDE1234F1Z5",
                        "PAN: ABCDE1234F",
                        "Registration Date: 10/04/2024",
                        "Expiry Date: 10/04/2028"
                )
        );

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(100);

        DocumentTextResult result = textExtractionService.extractText(pdfBytes);

        assertThat(result).isNotNull();
        assertThat(result.isHasTextLayer()).isTrue();
        assertThat(result.getPageCount()).isEqualTo(1);
        assertThat(result.getFullText()).contains("SYNTHETIC DEMONSTRATION DOCUMENT");
        assertThat(result.getFullText()).contains("FOR SIH26100 PROTOTYPE TESTING ONLY");
        assertThat(result.getFullText()).contains("GSTIN: 29ABCDE1234F1Z5");
        assertThat(result.getFullText()).contains("PAN: ABCDE1234F");
        assertThat(result.getFullText()).contains("Legal Name: Apex Secure Systems Pvt Ltd");
    }

}
