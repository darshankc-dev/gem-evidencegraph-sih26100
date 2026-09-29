# GeM EvidenceGraph

### AI-Powered Integrated Bid Compliance Verification Platform for GeM Procurement

**SIH 2026 — Problem Statement: SIH26100**

> **From Documents to Evidence to Explainable Procurement Decisions**

GeM EvidenceGraph is an evidence-centric bid compliance verification platform designed to help procurement officers analyze bidder submissions through a structured chain of:

**Document → Claim → Evidence → Verification → Entity Resolution → Temporal Validation → Compliance → Risk → Decision**

Instead of treating a tender submission as a collection of disconnected documents, GeM EvidenceGraph transforms extracted information into an auditable evidence graph and evaluates the bid through deterministic verification, contradiction detection, temporal validation, compliance rules, and multi-dimensional risk assessment.

---

## ⚡Why GeM EvidenceGraph?

Procurement verification often requires officers to examine multiple documents, compare bidder information, validate eligibility requirements, identify inconsistencies, and determine whether submitted evidence is valid at the relevant point in time.

A simple document parser is not enough.

GeM EvidenceGraph addresses this by separating:

- **What the bidder claimed**
- **What evidence supports the claim**
- **Whether the evidence could be verified**
- **Whether different sources contradict each other**
- **Whether the evidence was valid at the bid date**
- **Whether mandatory tender requirements are satisfied**
- **What risks require human attention**

The system therefore does not simply produce a "pass/fail" result.

It produces an **auditable decision trail**.

---

# 🎯 Problem Statement

### SIH26100

**AI-Powered Integrated Bid Compliance Verification Platform for GeM Procurement**

The platform is designed to support integrated verification of bidder submissions against tender-specific requirements by connecting extracted claims, supporting evidence, verification results, temporal validity, contradictions, and compliance outcomes.

---

# 💡 Our Approach

GeM EvidenceGraph is built around an **EvidenceGraph architecture**.

### Core pipeline

```text
                    ┌─────────────────────┐
                    │   Tender & Bid      │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │     Documents       │
                    │ PDF / Evidence      │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Claim Extraction    │
                    │ GSTIN / PAN / UDYAM │
                    │ Names / Dates / etc │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │      Evidence       │
                    │ Provenance + Source │
                    └──────────┬──────────┘
                               │
                 ┌─────────────┼─────────────┐
                 ▼             ▼             ▼
          Verification   Entity Resolution  Temporal
             Gateway       & Contradictions  Validation
                 │             │             │
                 └─────────────┼─────────────┘
                               ▼
                    ┌─────────────────────┐
                    │ Compliance Engine   │
                    │ Tender Requirements │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Risk Assessment     │
                    │ 5 Risk Dimensions   │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │ Explainable         │
                    │ Decision            │
                    └─────────────────────┘
🔍 Key Innovation
EvidenceGraph

The central design principle is:

Claims are not treated as verified facts.

For example:

Document
   │
   ▼
Claim: GSTIN = XXXXXXXXXXXXXXX
   │
   ▼
Verification
   │
   ├── VERIFIED
   │
   └── MISMATCH

This separation allows the system to distinguish between:

Extracted information
Verified information
Contradictory information
Missing information
Temporally invalid information
Information requiring human review

This creates an auditable path from the original document to the final decision.

🧠 Core Features
1. Document Ingestion

Supports controlled ingestion of bidder documents with:

PDF
PNG
JPG
JPEG
File size validation
MIME type validation
Extension validation
Executable signature rejection
Path traversal protection
SHA-256 document hashing
Duplicate detection
Document version tracking
Integrity status
2. Deterministic Document Extraction

The prototype extracts structured claims from documents using deterministic pattern-based extraction.

Current claim types include:

GSTIN
PAN
UDYAM Number
Legal Name
Address
Registration Date
Expiry Date
Certificate Number

Each claim stores information such as:

Field
Value
Normalized Value
Confidence
Extraction Method
Source Page
Source Location
Document

This preserves traceability between extracted information and its source document.

🔐 3. Verification Gateway

The verification layer is intentionally separated from extraction.

CLAIM
  │
  ▼
Verification Gateway
  │
  ├── GST Adapter
  ├── PAN Adapter
  ├── UDYAM Adapter
  └── Debarment Adapter

Verification results include:

VERIFIED
MISMATCH
UNVERIFIED
SOURCE_UNAVAILABLE
NOT_APPLICABLE
PENDING_HUMAN_REVIEW
Important design principle
SOURCE_UNAVAILABLE ≠ VERIFIED
UNVERIFIED ≠ VERIFIED
PENDING_HUMAN_REVIEW ≠ VERIFIED

The system never treats unavailable evidence as verified evidence.

Prototype integration model

The current prototype uses mock verification adapters to demonstrate the verification architecture.

These adapters are designed as replaceable components so that authorized production integrations can be connected later.

🔗 4. Evidence Relationships

Evidence can be connected through explicit relationships.

Examples:

CLAIM
  │
  ├── VERIFIED_BY ───────► EVIDENCE
  │
  └── CONTRADICTS ───────► EVIDENCE

This allows the system to preserve the reasoning behind verification outcomes rather than storing only a final status.

🧩 5. Deterministic Entity Resolution

Bidder identity is resolved using normalized attributes rather than opaque similarity scores.

Strong identifiers
PAN
GSTIN
UDYAM Number
Supporting attributes
Legal Name
Address
Email
Phone

The system normalizes:

Legal suffixes
Names
Addresses
PAN
GSTIN
UDYAM numbers
Email
Phone

Possible outcomes include:

MATCH
PROBABLE_MATCH
MISMATCH
INSUFFICIENT_DATA

Missing information does not automatically produce a mismatch.

⏳ 6. Temporal Verification

Evidence can be correct today but invalid at the time a bid was submitted.

GeM EvidenceGraph therefore evaluates temporal validity against relevant procurement dates.

Temporal statuses
VALID_AT_BID_DATE
EXPIRED_AT_BID_DATE
NOT_YET_VALID_AT_BID_DATE
ISSUED_AFTER_BID
ISSUED_AFTER_TENDER_PUBLICATION
CONFLICTING_DATES
MISSING_TEMPORAL_DATA
NOT_APPLICABLE

The system distinguishes:

Issue Date
Observed At
Validity Start
Validity End
Bid Date
Tender Publication Date

This prevents a document's retrieval timestamp from being incorrectly treated as its issuance date.

📋 7. Tender-Specific Compliance Engine

Requirements are evaluated against the actual tender context.

Architecture:

TENDER
   │
   ▼
REQUIREMENTS
   │
   ▼
BID
   │
   ├── CLAIMS
   └── EVIDENCE
          │
          ▼
      RULE ENGINE
          │
          ▼
  REQUIREMENT RESULTS
          │
          ▼
OVERALL COMPLIANCE
Compliance statuses
COMPLIANT
NON_COMPLIANT
MISSING
UNVERIFIED
CONTRADICTORY
NOT_APPLICABLE
PENDING_HUMAN_REVIEW
Example requirements
GST registration
PAN availability
UDYAM registration
OEM requirement
Make-in-India requirement
Required declarations
Eligibility criteria
Mandatory documents

The engine does not automatically disqualify a bidder merely because evidence is unavailable or ambiguous.

⚠️ 8. Multi-Dimensional Risk Assessment

GeM EvidenceGraph evaluates risk across five dimensions:

┌─────────────────────────┐
│ Eligibility              │
├─────────────────────────┤
│ Document Integrity       │
├─────────────────────────┤
│ Consistency              │
├─────────────────────────┤
│ Temporal                 │
├─────────────────────────┤
│ Verification             │
└─────────────────────────┘

Each dimension generates findings with severity levels:

INFO
LOW
MEDIUM
HIGH
CRITICAL

The overall risk level is derived from the highest relevant dimension score.

🧠 9. Explainable Decision Engine

The final decision is not based on a black-box classification.

The system evaluates:

Compliance results
Verification status
Contradictions
Temporal issues
Document integrity
Eligibility findings
Evidence completeness
Risk findings
Human-review conditions
Decision outcomes
RECOMMEND
REVIEW
HIGH_RISK
INSUFFICIENT_EVIDENCE

The platform is designed as decision support, not as an autonomous procurement authority.

The final procurement decision remains with the authorized procurement officer.

📊 Demonstration Scenarios

The project contains a synthetic demonstration dataset designed to exercise the complete verification pipeline.

Tender
GEM-DEMO-2026-001
Demonstration bids
Bid	Scenario	Compliance	Risk	Decision
Apex	Consistent and valid evidence	COMPLIANT	LOW	RECOMMEND
BluePeak	Contradictory and temporally problematic evidence	REVIEW	HIGH	REVIEW
Crestline	Multiple serious compliance/integrity findings	NON_COMPLIANT	HIGH	HIGH_RISK

The results are generated through the implemented ingestion, extraction, verification, compliance, temporal, and risk pipeline rather than hardcoded dashboard results.

🧪 Demo Dataset Design

The demonstration dataset contains:

1 tender
3 bids
15 documents
Tender-specific requirements
Extracted claims
Evidence records
Verification results
Temporal evaluations
Compliance evaluations
Risk assessments

The demo documents are synthetically generated for demonstration purposes.

They are clearly treated as synthetic demo evidence, not real government records.

🖥️ Judge Dashboard

The project includes a judge-facing dashboard designed to expose the complete analysis pipeline.

Dashboard includes
SIH 2026 branding
PS ID
Live application health
Demo dataset loading/reset
KPI cards
Bid analysis matrix
EvidenceGraph visualization
Decision summary
Compliance analysis
Risk assessment
Temporal verification
Claims
Evidence relationships
Documents
SHA-256 integrity information
Extraction status
Verification status
High-level UI flow
Dashboard
   │
   ├── Overview
   ├── Compliance
   ├── Risk Assessment
   ├── Temporal Verification
   ├── Claims
   ├── Evidence & Graph Topology
   └── Documents
🏗️ Technology Stack
Backend
Java 17
Spring Boot 3.3.4
Spring MVC
Spring Data JPA
Hibernate
Spring Security
Maven
Database
PostgreSQL
Document Processing
Apache PDFBox
Frontend
Thymeleaf
HTML
CSS
JavaScript
Deployment
Docker
Render
PostgreSQL
🗄️ Database Architecture

The application uses a relational PostgreSQL schema with entities covering the complete evidence lifecycle.

Core tables include:

bidders
tenders
bids
documents
claims
evidences
evidence_relationships
compliance_requirements
entity_resolution_results
bid_compliance_evaluations
temporal_evaluations
bid_risk_assessments
risk_assessments
risk_findings

Hibernate schema auto-generation is disabled.

The application uses an explicit schema.sql so that database structure remains controlled and reproducible.

🔄 End-to-End Processing Pipeline
1. Upload Bid Documents
          ↓
2. Validate File
          ↓
3. Calculate SHA-256
          ↓
4. Extract Document Text
          ↓
5. Extract Claims
          ↓
6. Create Evidence
          ↓
7. Verify Claims
          ↓
8. Resolve Bidder Identity
          ↓
9. Detect Contradictions
          ↓
10. Validate Temporal Evidence
          ↓
11. Evaluate Tender Requirements
          ↓
12. Calculate Risk Dimensions
          ↓
13. Generate Explainable Decision
          ↓
14. Present Evidence Trail
🔌 API Overview
Health
GET /api/health

Checks application/database health.

Demo Dataset
POST /api/demo/load
POST /api/demo/reset
GET  /api/demo/status
Documents
POST /api/bids/{bidId}/documents
GET  /api/documents/{documentId}
GET  /api/bids/{bidId}/documents
Extraction
POST /api/documents/{documentId}/extract
GET  /api/documents/{documentId}/claims
Verification
POST /api/claims/{claimId}/verify
POST /api/bids/{bidId}/verify
Entity Resolution
POST /api/entity-resolution/compare
POST /api/entity-resolution/bidders/{bidderId}/resolve
Compliance
POST /api/tenders/{tenderId}/requirements
GET  /api/tenders/{tenderId}/requirements
GET  /api/requirements/{requirementId}

POST /api/bids/{bidId}/compliance/evaluate
GET  /api/bids/{bidId}/compliance
Temporal Verification
POST /api/bids/{bidId}/temporal/evaluate
GET  /api/bids/{bidId}/temporal
GET  /api/evidence/{evidenceId}/temporal
Risk Assessment
POST /api/bids/{bidId}/risk/evaluate
GET  /api/bids/{bidId}/risk
GET  /api/bids/{bidId}/risk/dimensions
🚀 Running Locally
Prerequisites

Install:

Java 17
Maven 3.9+
PostgreSQL
Git

Verify:

java -version
mvn -version
psql --version
Clone Repository
git clone https://github.com/darshankc-dev/gem-evidencegraph-sih26100.git
cd gem-evidencegraph-sih26100
Configure Database

Create a PostgreSQL database:

gem_evidencegraph

Configure environment variables:

DB_URL=jdbc:postgresql://localhost:5432/gem_evidencegraph
DB_USERNAME=postgres
DB_PASSWORD=<your-password>
DB_DRIVER=org.postgresql.Driver

Do not commit credentials to Git.

Initialize Database

Execute:

src/main/resources/schema.sql

against the PostgreSQL database.

The project intentionally does not depend on data.sql for its demonstration workflow.

Run Application
mvn spring-boot:run

Application:

http://localhost:8080

Health:

http://localhost:8080/api/health
🐳 Docker

The project includes a production-oriented multi-stage Dockerfile.

Build:

docker build -t gem-evidencegraph .

Run:

docker run -p 8080:8080 \
  -e DB_URL="<database-url>" \
  -e DB_USERNAME="<database-user>" \
  -e DB_PASSWORD="<database-password>" \
  -e DB_DRIVER="org.postgresql.Driver" \
  gem-evidencegraph
☁️ Deployment

The prototype is deployable as a Docker-based Spring Boot web service.

Current deployment architecture:

                    Internet
                       │
                       ▼
             ┌───────────────────┐
             │ Render Web Service│
             │ Spring Boot       │
             │ Docker            │
             └─────────┬─────────┘
                       │
                       │ Internal
                       │ PostgreSQL
                       ▼
             ┌───────────────────┐
             │ Render PostgreSQL │
             │ gem_evidencegraph │
             └───────────────────┘

Database credentials are supplied through environment variables.

No credentials are stored in the repository.

🧪 Testing

The project has been developed with extensive automated testing across:

Domain model
Document ingestion
Document extraction
Verification
Evidence relationships
Entity resolution
Compliance rules
Temporal validation
Risk evaluation
Decision engine
Demo pipeline
Controllers
Services

The final local test suite reached:

248 tests
248 passed
0 failures
0 errors
🔐 Security & Integrity Principles

The project incorporates several security and integrity controls:

File Security
MIME validation
Extension validation
Executable signature rejection
Path traversal protection
File size limits
SHA-256 hashing
Duplicate detection
Database Security

Database credentials are provided through environment variables.

Credentials are not committed to GitHub.

Evidence Integrity

Documents are associated with SHA-256 hashes.

Evidence stores provenance-related information such as:

Source system
Adapter version
Retrieval timestamp
Provenance hash
Verification status
🧭 Design Principles
1. Extraction ≠ Verification

Extracting a value from a document does not make the value trustworthy.

2. Missing ≠ False

Missing information is handled separately from contradictory information.

3. Unavailable ≠ Verified

If an external verification source is unavailable, the system does not treat the claim as verified.

4. Temporal Context Matters

Evidence is evaluated against the relevant bid and tender dates.

5. Explainability

Every important outcome is backed by findings and evidence rather than an unexplained score.

6. Human-in-the-Loop

The platform provides procurement decision support.

It does not replace the authorized procurement officer.

🧱 Project Structure
gem-evidencegraph-sih26100/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ...
│   │   │
│   │   └── resources/
│   │       ├── templates/
│   │       ├── static/
│   │       ├── application.yml
│   │       └── schema.sql
│   │
│   └── test/
│
├── docs/
│
├── Dockerfile
├── pom.xml
├── .gitignore
└── README.md
📌 Current Prototype Scope

The current implementation demonstrates:

Document ingestion
Deterministic extraction
Claim creation
Evidence generation
Verification gateway
Mock verification adapters
Entity resolution
Contradiction handling
Temporal verification
Tender-specific compliance
Multi-dimensional risk assessment
Explainable decision generation
Synthetic end-to-end demonstration
Judge-facing dashboard
Docker deployment
PostgreSQL persistence
🔭 Future Scope

The architecture is designed to support future extensions such as:

Authorized Government Integrations

Replace mock verification adapters with authorized integrations where APIs or approved interfaces are available.

Mock Adapter
     ↓
Adapter Interface
     ↓
Authorized External Verification Source
Advanced Document Intelligence

The extraction layer can be extended with more sophisticated document understanding while retaining the existing Claim → Evidence architecture.

Additional Evidence Sources

Future adapters can support additional procurement verification sources without rewriting the core compliance engine.

Advanced Graph Analytics

The EvidenceGraph model can be extended to identify relationships across bidders, documents, claims, evidence, and verification sources at larger scale.

⚠️ Prototype Limitations

This repository represents a working prototype.

The following points are intentionally explicit:

Government verification adapters used in the demonstration are mock adapters.
Demonstration documents are synthetic.
The prototype does not claim live access to government verification systems.
Production deployment would require authorized integrations, authentication, access controls, audit policies, and appropriate government/system approvals.
Final procurement decisions remain the responsibility of authorized procurement officers.

🏆 Why This Architecture?

The platform is designed around a simple principle:

A procurement decision should be traceable back to the evidence that produced it.

Instead of:

Document → AI Score → Decision

GeM EvidenceGraph uses:

Document
   ↓
Claim
   ↓
Evidence
   ↓
Verification
   ↓
Relationship
   ↓
Temporal Validation
   ↓
Compliance
   ↓
Risk
   ↓
Explainable Decision

This makes the reasoning process inspectable and supports human review when evidence is incomplete, contradictory, unavailable, or temporally invalid.

📎 Repository

GitHub Repository

https://github.com/darshankc-dev/gem-evidencegraph-sih26100

👥 Project

GeM EvidenceGraph

Smart India Hackathon 2026

Problem Statement: SIH26100

Domain: GeM Procurement / AI-assisted Compliance Verification

📜 License

This project is developed as a Smart India Hackathon prototype.

See the repository for applicable licensing and usage information.


### One thing I'd add at the very top

For the actual GitHub page, I'd make the opening even stronger with badges:

```markdown
# GeM EvidenceGraph

[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue)](https://www.docker.com/)
[![SIH 2026](https://img.shields.io/badge/SIH-2026-red)](https://www.sih.gov.in/)

### AI-Powered Integrated Bid Compliance Verification Platform for GeM Procurement

**SIH26100 | EvidenceGraph-based verification | Explainable compliance | Temporal validation | Risk assessment**

> **Document → Claim → Evidence → Verification → Compliance → Risk → Decision**

[🚀 Live Demo](https://gem-evidencegraph-sih26100.onrender.com/) · [📂 Source Code](https://github.com/darshankc-dev/gem-evidencegraph-sih26100)
