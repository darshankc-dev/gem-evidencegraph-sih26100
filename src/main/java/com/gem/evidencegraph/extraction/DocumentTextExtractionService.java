package com.gem.evidencegraph.extraction;

import java.nio.file.Path;

public interface DocumentTextExtractionService {

    DocumentTextResult extractText(Path filePath);

    DocumentTextResult extractText(byte[] fileBytes);

}
