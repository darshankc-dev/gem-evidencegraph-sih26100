package com.gem.evidencegraph.service;

import com.gem.evidencegraph.util.FileStorageException;
import com.gem.evidencegraph.util.FileValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentStorageServiceTest {

    @TempDir
    Path tempStorageDir;

    private DocumentStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new DocumentStorageServiceImpl(tempStorageDir.toString());
    }

    @Test
    @DisplayName("Should physically store file with UUID name in storage directory")
    void shouldStoreFileSuccessfully() throws IOException {
        UUID documentId = UUID.randomUUID();
        byte[] content = "Sample PDF content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                content
        );

        String storedPath = storageService.store(documentId, "invoice.pdf", file);

        assertNotNull(storedPath);
        Path physicalPath = Path.of(storedPath);
        assertTrue(Files.exists(physicalPath));
        assertEquals(documentId + ".pdf", physicalPath.getFileName().toString());
        assertArrayEquals(content, Files.readAllBytes(physicalPath));
    }

    @Test
    @DisplayName("Should reject filename with path traversal characters")
    void shouldRejectPathTraversalInFilename() {
        UUID documentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../malicious.pdf",
                "application/pdf",
                "test".getBytes()
        );

        FileValidationException ex = assertThrows(FileValidationException.class, () ->
                storageService.store(documentId, "../../malicious.pdf", file)
        );
        assertTrue(ex.getMessage().contains("path sequence"));
    }

    @Test
    @DisplayName("Should reject empty or null file")
    void shouldRejectEmptyFile() {
        UUID documentId = UUID.randomUUID();
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThrows(FileValidationException.class, () ->
                storageService.store(documentId, "empty.pdf", emptyFile)
        );
    }

    @Test
    @DisplayName("Should delete stored file properly")
    void shouldDeleteStoredFile() {
        UUID documentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "certificate.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        String storedPath = storageService.store(documentId, "certificate.png", file);
        assertTrue(Files.exists(Path.of(storedPath)));

        storageService.delete(storedPath);
        assertFalse(Files.exists(Path.of(storedPath)));
    }

}
