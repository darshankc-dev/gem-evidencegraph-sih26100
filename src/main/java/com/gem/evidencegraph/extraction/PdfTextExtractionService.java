package com.gem.evidencegraph.extraction;

import com.gem.evidencegraph.util.FileStorageException;
import com.gem.evidencegraph.util.FileValidationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PdfTextExtractionService implements DocumentTextExtractionService {

    @Override
    public DocumentTextResult extractText(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            throw new FileStorageException("Target document file does not exist on disk: " + filePath);
        }

        File file = filePath.toFile();
        try (PDDocument document = Loader.loadPDF(file)) {
            return processDocument(document);
        } catch (IOException e) {
            throw new FileValidationException("Malformed or unreadable PDF document at " + filePath.getFileName() + ": " + e.getMessage());
        }
    }

    @Override
    public DocumentTextResult extractText(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new FileValidationException("File byte array is null or empty");
        }

        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            return processDocument(document);
        } catch (IOException e) {
            throw new FileValidationException("Malformed or unreadable PDF document bytes: " + e.getMessage());
        }
    }

    private DocumentTextResult processDocument(PDDocument document) throws IOException {
        int pageCount = document.getNumberOfPages();
        if (pageCount == 0) {
            return DocumentTextResult.builder()
                    .pageCount(0)
                    .totalCharacterCount(0)
                    .fullText("")
                    .hasTextLayer(false)
                    .build();
        }

        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);

        StringBuilder fullTextBuilder = new StringBuilder();
        Map<Integer, String> pageTexts = new LinkedHashMap<>();

        for (int p = 1; p <= pageCount; p++) {
            stripper.setStartPage(p);
            stripper.setEndPage(p);
            String rawPageText = stripper.getText(document);
            String normalizedPageText = normalizeWhitespace(rawPageText);
            pageTexts.put(p, normalizedPageText);
            fullTextBuilder.append(normalizedPageText).append("\n");
        }

        String fullText = fullTextBuilder.toString().trim();
        boolean hasTextLayer = !fullText.isEmpty();

        return DocumentTextResult.builder()
                .pageCount(pageCount)
                .totalCharacterCount(fullText.length())
                .fullText(fullText)
                .pageTexts(pageTexts)
                .hasTextLayer(hasTextLayer)
                .build();
    }

    private String normalizeWhitespace(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .trim();
    }

}
