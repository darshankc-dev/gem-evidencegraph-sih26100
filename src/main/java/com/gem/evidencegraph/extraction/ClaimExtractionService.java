package com.gem.evidencegraph.extraction;

import com.gem.evidencegraph.entity.Claim;
import com.gem.evidencegraph.entity.Document;

import java.util.List;

public interface ClaimExtractionService {

    List<Claim> extractClaims(Document document, DocumentTextResult textResult);

}
