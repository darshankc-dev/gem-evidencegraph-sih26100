CREATE TABLE IF NOT EXISTS bidders (
    id UUID PRIMARY KEY,
    legal_name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255),
    pan VARCHAR(20),
    gstin VARCHAR(30),
    udyam_number VARCHAR(50),
    registered_address TEXT,
    normalized_address TEXT,
    email VARCHAR(255),
    phone VARCHAR(30),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_bidder_pan ON bidders(pan);
CREATE INDEX IF NOT EXISTS idx_bidder_gstin ON bidders(gstin);
CREATE INDEX IF NOT EXISTS idx_bidder_udyam ON bidders(udyam_number);
CREATE INDEX IF NOT EXISTS idx_bidder_normalized_name ON bidders(normalized_name);

CREATE TABLE IF NOT EXISTS tenders (
    id UUID PRIMARY KEY,
    tender_reference VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    publication_date TIMESTAMP WITHOUT TIME ZONE,
    submission_deadline TIMESTAMP WITHOUT TIME ZONE,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_tender_reference ON tenders(tender_reference);
CREATE INDEX IF NOT EXISTS idx_tender_status ON tenders(status);

CREATE TABLE IF NOT EXISTS bids (
    id UUID PRIMARY KEY,
    bid_reference VARCHAR(255) NOT NULL UNIQUE,
    tender_id UUID NOT NULL REFERENCES tenders(id),
    bidder_id UUID NOT NULL REFERENCES bidders(id),
    submission_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_bid_reference ON bids(bid_reference);
CREATE INDEX IF NOT EXISTS idx_bid_tender ON bids(tender_id);
CREATE INDEX IF NOT EXISTS idx_bid_bidder ON bids(bidder_id);
CREATE INDEX IF NOT EXISTS idx_bid_status ON bids(status);

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    bid_id UUID NOT NULL REFERENCES bids(id),
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255),
    storage_path VARCHAR(500),
    content_type VARCHAR(100),
    file_size BIGINT,
    sha256_hash VARCHAR(64),
    document_type VARCHAR(50) NOT NULL,
    uploaded_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    version INTEGER DEFAULT 1,
    extraction_status VARCHAR(50) NOT NULL,
    integrity_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_doc_bid_id ON documents(bid_id);
CREATE INDEX IF NOT EXISTS idx_document_sha256 ON documents(sha256_hash);
CREATE INDEX IF NOT EXISTS idx_doc_type ON documents(document_type);
CREATE INDEX IF NOT EXISTS idx_doc_extraction ON documents(extraction_status);

CREATE TABLE IF NOT EXISTS claims (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    field_name VARCHAR(255) NOT NULL,
    field_value TEXT,
    normalized_value TEXT,
    confidence DOUBLE PRECISION,
    extraction_method VARCHAR(50) NOT NULL,
    source_page INTEGER,
    source_location VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_claim_field_name ON claims(field_name);
CREATE INDEX IF NOT EXISTS idx_claim_normalized_val ON claims(normalized_value);
CREATE INDEX IF NOT EXISTS idx_claim_document ON claims(document_id);

CREATE TABLE IF NOT EXISTS evidences (
    id UUID PRIMARY KEY,
    bid_id UUID REFERENCES bids(id),
    claim_id UUID REFERENCES claims(id),
    evidence_type VARCHAR(50) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_reference VARCHAR(255),
    source_system VARCHAR(100),
    adapter_version VARCHAR(50),
    subject VARCHAR(255),
    attribute VARCHAR(100),
    "value" TEXT,
    normalized_value TEXT,
    verification_status VARCHAR(50) NOT NULL,
    observed_at TIMESTAMP WITHOUT TIME ZONE,
    valid_from TIMESTAMP WITHOUT TIME ZONE,
    valid_until TIMESTAMP WITHOUT TIME ZONE,
    retrieved_at TIMESTAMP WITHOUT TIME ZONE,
    confidence DOUBLE PRECISION,
    provenance_hash VARCHAR(64),
    raw_response_snapshot TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_evidence_claim_id ON evidences(claim_id);
CREATE INDEX IF NOT EXISTS idx_evidence_bid_id ON evidences(bid_id);
CREATE INDEX IF NOT EXISTS idx_evidence_source_type ON evidences(source_type);
CREATE INDEX IF NOT EXISTS idx_evidence_verif_status ON evidences(verification_status);
CREATE INDEX IF NOT EXISTS idx_evidence_normalized_val ON evidences(normalized_value);

CREATE TABLE IF NOT EXISTS evidence_relationships (
    id UUID PRIMARY KEY,
    source_claim_id UUID REFERENCES claims(id),
    source_evidence_id UUID REFERENCES evidences(id),
    target_evidence_id UUID NOT NULL REFERENCES evidences(id),
    relationship_type VARCHAR(50) NOT NULL,
    reason TEXT,
    confidence DOUBLE PRECISION,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_ev_rel_claim ON evidence_relationships(source_claim_id);
CREATE INDEX IF NOT EXISTS idx_ev_rel_source ON evidence_relationships(source_evidence_id);
CREATE INDEX IF NOT EXISTS idx_ev_rel_target ON evidence_relationships(target_evidence_id);
CREATE INDEX IF NOT EXISTS idx_ev_rel_type ON evidence_relationships(relationship_type);

CREATE TABLE IF NOT EXISTS compliance_requirements (
    id UUID PRIMARY KEY,
    tender_id UUID NOT NULL REFERENCES tenders(id),
    requirement_code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    requirement_type VARCHAR(50) NOT NULL,
    mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    expected_source_type VARCHAR(50),
    expected_document_type VARCHAR(50),
    validation_rule TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_comp_req_tender ON compliance_requirements(tender_id);
CREATE INDEX IF NOT EXISTS idx_comp_req_code ON compliance_requirements(requirement_code);

CREATE TABLE IF NOT EXISTS entity_resolution_results (
    id UUID PRIMARY KEY,
    bidder_id UUID REFERENCES bidders(id),
    compared_source VARCHAR(100),
    match_status VARCHAR(30) NOT NULL,
    confidence DOUBLE PRECISION,
    matched_attributes VARCHAR(500),
    mismatched_attributes VARCHAR(500),
    missing_attributes VARCHAR(500),
    explanation TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_err_bidder_id ON entity_resolution_results(bidder_id);
CREATE INDEX IF NOT EXISTS idx_err_match_status ON entity_resolution_results(match_status);

CREATE TABLE IF NOT EXISTS bid_compliance_evaluations (
    id UUID PRIMARY KEY,
    bid_id UUID NOT NULL REFERENCES bids(id),
    overall_status VARCHAR(30) NOT NULL,
    compliance_percentage DOUBLE PRECISION,
    mandatory_requirement_count INTEGER NOT NULL,
    compliant_count INTEGER NOT NULL,
    non_compliant_count INTEGER NOT NULL,
    missing_count INTEGER NOT NULL,
    unverified_count INTEGER NOT NULL,
    contradictory_count INTEGER NOT NULL,
    pending_review_count INTEGER NOT NULL,
    not_applicable_count INTEGER NOT NULL,
    evaluation_details_json TEXT,
    summary TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_bce_bid_id ON bid_compliance_evaluations(bid_id);
CREATE INDEX IF NOT EXISTS idx_bce_overall_status ON bid_compliance_evaluations(overall_status);

CREATE TABLE IF NOT EXISTS temporal_evaluations (
    id UUID PRIMARY KEY,
    bid_id UUID NOT NULL REFERENCES bids(id),
    tender_id UUID REFERENCES tenders(id),
    evidence_id UUID NOT NULL REFERENCES evidences(id),
    reference_date TIMESTAMP WITHOUT TIME ZONE,
    valid_from TIMESTAMP WITHOUT TIME ZONE,
    valid_until TIMESTAMP WITHOUT TIME ZONE,
    observed_at TIMESTAMP WITHOUT TIME ZONE,
    issue_date TIMESTAMP WITHOUT TIME ZONE,
    status VARCHAR(50) NOT NULL,
    reason TEXT,
    rule_applied VARCHAR(100),
    evaluated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_temp_eval_bid ON temporal_evaluations(bid_id);
CREATE INDEX IF NOT EXISTS idx_temp_eval_tender ON temporal_evaluations(tender_id);
CREATE INDEX IF NOT EXISTS idx_temp_eval_evidence ON temporal_evaluations(evidence_id);
CREATE INDEX IF NOT EXISTS idx_temp_eval_status ON temporal_evaluations(status);

CREATE TABLE IF NOT EXISTS bid_risk_assessments (
    id UUID PRIMARY KEY,
    bid_id UUID NOT NULL REFERENCES bids(id),
    overall_risk_level VARCHAR(30) NOT NULL,
    overall_risk_score INTEGER NOT NULL,
    decision_recommendation VARCHAR(50) NOT NULL,
    hard_fail BOOLEAN NOT NULL DEFAULT FALSE,
    summary TEXT,
    reason TEXT,
    evaluated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_bra_bid_id ON bid_risk_assessments(bid_id);
CREATE INDEX IF NOT EXISTS idx_bra_risk_level ON bid_risk_assessments(overall_risk_level);
CREATE INDEX IF NOT EXISTS idx_bra_recommendation ON bid_risk_assessments(decision_recommendation);

CREATE TABLE IF NOT EXISTS risk_assessments (
    id UUID PRIMARY KEY,
    bid_id UUID NOT NULL REFERENCES bids(id),
    bid_risk_assessment_id UUID REFERENCES bid_risk_assessments(id) ON DELETE CASCADE,
    dimension VARCHAR(50) NOT NULL,
    risk_level VARCHAR(30) NOT NULL,
    risk_score INTEGER NOT NULL,
    finding_count INTEGER NOT NULL DEFAULT 0,
    summary TEXT,
    reason TEXT,
    evaluated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_ra_bid_id ON risk_assessments(bid_id);
CREATE INDEX IF NOT EXISTS idx_ra_bra_id ON risk_assessments(bid_risk_assessment_id);
CREATE INDEX IF NOT EXISTS idx_ra_dimension ON risk_assessments(dimension);

CREATE TABLE IF NOT EXISTS risk_findings (
    id UUID PRIMARY KEY,
    risk_assessment_id UUID NOT NULL REFERENCES risk_assessments(id) ON DELETE CASCADE,
    bid_id UUID REFERENCES bids(id),
    dimension VARCHAR(50) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    code VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    source_type VARCHAR(50),
    source_id VARCHAR(255),
    rule_name VARCHAR(100),
    weight INTEGER NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_rf_assessment_id ON risk_findings(risk_assessment_id);
CREATE INDEX IF NOT EXISTS idx_rf_bid_id ON risk_findings(bid_id);
CREATE INDEX IF NOT EXISTS idx_rf_dimension ON risk_findings(dimension);
CREATE INDEX IF NOT EXISTS idx_rf_severity ON risk_findings(severity);
CREATE INDEX IF NOT EXISTS idx_rf_code ON risk_findings(code);
