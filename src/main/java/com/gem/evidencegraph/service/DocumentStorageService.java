package com.gem.evidencegraph.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

public interface DocumentStorageService {

    String store(UUID documentId, String originalFilename, MultipartFile file);

    Path getStorageDirectory();

    void delete(String storagePath);

}
