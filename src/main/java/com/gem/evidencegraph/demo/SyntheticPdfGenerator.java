package com.gem.evidencegraph.demo;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public final class SyntheticPdfGenerator {

    private SyntheticPdfGenerator() {

    }

    public static byte[] generatePdf(String documentTitle, List<String> lines) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
                contentStream.setLeading(14f);
                contentStream.newLineAtOffset(50, 760);
                contentStream.showText("SYNTHETIC DEMONSTRATION DOCUMENT");
                contentStream.newLine();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                contentStream.showText("FOR SIH26100 PROTOTYPE TESTING ONLY - NOT AN OFFICIAL GOVERNMENT DOCUMENT");
                contentStream.newLine();
                contentStream.newLine();

                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 13);
                contentStream.setLeading(18f);
                contentStream.showText(sanitizeText(documentTitle));
                contentStream.newLine();
                contentStream.newLine();

                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                contentStream.setLeading(16f);

                if (lines != null) {
                    for (String line : lines) {
                        if (line == null || line.isBlank()) {
                            contentStream.newLine();
                        } else {
                            contentStream.showText(sanitizeText(line));
                            contentStream.newLine();
                        }
                    }
                }

                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate synthetic PDF document: " + e.getMessage(), e);
        }
    }

    private static String sanitizeText(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("[^\\x20-\\x7E]", " ").trim();
    }

}
