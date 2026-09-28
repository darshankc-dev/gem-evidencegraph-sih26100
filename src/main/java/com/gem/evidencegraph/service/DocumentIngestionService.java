package com.gem.evidencegraph.service;

import com.gem.evidencegraph.dto.DocumentResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentIngestionService {

    DocumentResponseDto uploadDocument(UUID bidId, MultipartFile file);

    DocumentResponseDto getDocumentMetadata(UUID documentId);

    List<DocumentResponseDto> getDocumentsByBid(UUID bidId);

}
