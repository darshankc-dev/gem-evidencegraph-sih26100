package com.gem.evidencegraph.service;

import com.gem.evidencegraph.util.FileStorageException;
import com.gem.evidencegraph.util.FileValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class DocumentStorageServiceImpl implements DocumentStorageService {

    private final Path rootStoragePath;

    public DocumentStorageServiceImpl(@Value("${app.storage.document-path:storage/documents}") String documentStoragePath) {
        this.rootStoragePath = Paths.get(documentStoragePath).toAbsolutePath().normalize();
        initializeDirectory();
    }

    private void initializeDirectory() {
        try {
            Files.createDirectories(this.rootStoragePath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to initialize document storage directory at " + rootStoragePath, e);
        }
    }

    @Override
    public String store(UUID documentId, String originalFilename, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("Uploaded file cannot be null or empty");
        }

        String cleanedOriginalFilename = StringUtils.cleanPath(originalFilename != null ? originalFilename : "");
        if (cleanedOriginalFilename.contains("..") || cleanedOriginalFilename.contains("/") || cleanedOriginalFilename.contains("\\")) {
            throw new FileValidationException("Filename contains invalid path sequence: " + originalFilename);
        }

        String extension = getFileExtension(cleanedOriginalFilename);
        String serverFileName = documentId.toString() + extension;

        Path destinationFile = this.rootStoragePath.resolve(serverFileName).normalize();
        if (!destinationFile.startsWith(this.rootStoragePath)) {
            throw new FileValidationException("Cannot store file outside current storage directory");
        }

        if (Files.exists(destinationFile)) {
            throw new FileStorageException("A file with the generated identifier already exists: " + serverFileName);
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return destinationFile.toString();
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file " + serverFileName + " to disk", e);
        }
    }

    @Override
    public Path getStorageDirectory() {
        return this.rootStoragePath;
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            Path fileToDelete = Paths.get(storagePath).normalize();
            if (fileToDelete.startsWith(this.rootStoragePath)) {
                Files.deleteIfExists(fileToDelete);
            }
        } catch (IOException ignored) {

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
