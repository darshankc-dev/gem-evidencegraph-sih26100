package com.gem.evidencegraph.util;

import lombok.Getter;

import java.util.UUID;

@Getter
public class DuplicateDocumentException extends RuntimeException {

    private final String sha256Hash;
    private final UUID existingDocumentId;

    public DuplicateDocumentException(String message, String sha256Hash, UUID existingDocumentId) {
        super(message);
        this.sha256Hash = sha256Hash;
        this.existingDocumentId = existingDocumentId;
    }

}
