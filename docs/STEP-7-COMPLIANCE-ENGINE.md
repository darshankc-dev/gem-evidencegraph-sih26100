# Step 7 — Tender-Specific Compliance Engine

## 1. Purpose

The **Tender-Specific Compliance Engine** is a deterministic, rule-based evaluation system designed for the GeM EvidenceGraph (SIH26100) platform. In public procurement, compliance cannot be determined in a vacuum (e.g., merely asking *"Does the bidder have a GST certificate?"*). Instead, the system must evaluate:

> **"Does this particular tender mandate GST registration, and if so, does this bid contain valid, verified, and non-contradictory evidence satisfying that requirement?"**

The engine evaluates a submitted `Bid` against the specific `ComplianceRequirement` records attached to its `Tender`. It produces transparent, granular, requirement-level findings and an aggregate advisory compliance assessment, strictly upholding the human decision boundary.

---

## 2. Tender-Aware Architecture

The system enforces a strict hierarchy where requirements are owned by the Tender, evaluated across Bid documents, claims, and verified evidence, and summarized for human procurement officers.

```
TENDER
  ↓
REQUIREMENTS
  ↓
BID
  ↓
CLAIMS + EVIDENCE
  ↓
RULE ENGINE
  ↓
REQUIREMENT RESULTS
  ↓
OVERALL COMPLIANCE ASSESSMENT
  ↓
PROCUREMENT OFFICER
```

### Key Architectural Invariants:
1. **Tender-Centric**: Requirements belong to the Tender (`Tender.complianceRequirements`), not hardcoded in Java classes.
2. **Evidence-Driven**: The engine operates on verified `Evidence` records created in Step 5 (Gateway Verification) and Step 6 (Entity Resolution), rather than unverified raw claims.
3. **No Automated Disqualification**: The engine never makes a final procurement qualification or disqualification decision; it produces an advisory assessment for the Procurement Officer.

---

## 3. Requirement Model

The domain model reuses and extends the existing `ComplianceRequirement` entity (`com.gem.evidencegraph.entity.ComplianceRequirement`):

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | `UUID` | Unique identifier (Primary Key) |
| `tender` | `Tender` | Associated Tender entity |
| `requirementCode` | `String` | Unique code within the tender (e.g., `REQ-GST`, `REQ-PAN`, `REQ-OEM`) |
| `name` | `String` | Human-readable title (e.g., `Valid GST Registration`) |
| `description` | `String` | Full description of requirement condition |
| `requirementType` | `RequirementType` | `REGISTRATION`, `DOCUMENT`, `DECLARATION`, `ELIGIBILITY`, `TEMPORAL`, `OTHER` |
| `mandatory` | `boolean` | `true` if mandatory for tender qualification; `false` if optional |
| `expectedSourceType` | `SourceType` | Expected verification gateway source (`GST`, `PAN`, `UDYAM`, `MCA`, etc.) |
| `expectedDocumentType` | `DocumentType` | Expected uploaded document type (`OEM_AUTHORIZATION`, `MAKE_IN_INDIA_DOCUMENT`, etc.) |
| `validationRule` | `String` | Optional rule expression or parameter (e.g., `MIN_EXPERIENCE_YEARS=3`) |

---

## 4. Compliance Statuses

The engine distinguishes strictly between separate states of evidence. The following statuses are defined in `ComplianceStatus`:

| Status | Definition | Example |
| :--- | :--- | :--- |
| **`COMPLIANT`** | Required evidence exists, is verified, and fully satisfies the requirement. | GST evidence is `VERIFIED` and active. |
| **`NON_COMPLIANT`** | Evidence clearly demonstrates failure or disqualification. | Bidder is on active debarment list; or experience is 1 yr when 3 yrs required. |
| **`MISSING`** | Required evidence or document was not submitted. | OEM authorization document is not present in the bid. |
| **`UNVERIFIED`** | Evidence exists in bid, but independent verification could not verify it. | External verification returned `SOURCE_UNAVAILABLE` or `UNVERIFIED`. |
| **`CONTRADICTORY`** | Conflicting evidence records exist across sources or verification returned mismatch. | GST verification status is `MISMATCH`, or multiple GST evidence records conflict. |
| **`NOT_APPLICABLE`** | Requirement does not apply to this specific bidder or bid. | Exemption condition met (e.g., startup exempt from prior turnover). |
| **`PENDING_HUMAN_REVIEW`** | Automated evaluation cannot safely determine compliance; requires officer judgment. | Unsupported custom rule format, or ambiguous eligibility clause. |

### Critical Distinction:
$$\text{MISSING} \neq \text{UNVERIFIED} \neq \text{CONTRADICTORY} \neq \text{NON\_COMPLIANT}$$

* *No GST certificate uploaded*: **`MISSING`**
* *GST certificate present, portal down*: **`UNVERIFIED`**
* *GST document PAN differs from verified PAN*: **`CONTRADICTORY`**
* *Debarred / clear failure of condition*: **`NON_COMPLIANT`**

---

## 5. Rule Engine Architecture

The compliance engine uses a strategy-style, extensible rule architecture via `ComplianceRule` and `ComplianceRuleEngine`.

### Core Interfaces and Classes:
* **`ComplianceRule`**: Defines `supports(ComplianceRequirement req)` and `evaluate(ComplianceRequirement req, BidEvaluationContext ctx)`.
* **`BidEvaluationContext`**: Pre-indexed context containing the `Bid`, `Tender`, `Bidder`, uploaded `Document` list (indexed by `DocumentType`), extracted `Claim` list (indexed by claim type), and verified `Evidence` list (indexed by `SourceType`).
* **`ComplianceRuleEngine`**: Spring `@Component` that discovers all registered `ComplianceRule` beans, matches requirements to appropriate rules, evaluates each requirement, and produces the overall assessment.

### Strategy Implementations:
1. **`RegistrationRequirementRule`**:
   - Handles `RequirementType.REGISTRATION` (GST, PAN, Udyam, Debarment list).
   - Inspects `Evidence` matching `expectedSourceType`.
   - Distinguishes `VERIFIED` (`COMPLIANT`), `MISMATCH` (`CONTRADICTORY`), `SOURCE_UNAVAILABLE` (`UNVERIFIED`), and missing evidence (`MISSING`).
   - Detects debarment: If `DEBARMENT_LIST` evidence is verified, marks as `NON_COMPLIANT`.
2. **`DocumentRequirementRule`**:
   - Handles `RequirementType.DOCUMENT` (e.g., OEM authorization, certifications).
   - Validates document presence and integrity (`FAILED` -> `NON_COMPLIANT`, `SUSPICIOUS` -> `PENDING_HUMAN_REVIEW`).
   - Checks associated claims and supporting evidence if available.
3. **`DeclarationRequirementRule`**:
   - Handles `RequirementType.DECLARATION` (e.g., Make in India, Non-Blacklisting declaration, Startup declaration).
   - Checks for presence of declaration document and verified declaration claims.
4. **`EligibilityRequirementRule`**:
   - Handles `RequirementType.ELIGIBILITY` (e.g., experience years, annual turnover).
   - Safely parses deterministic numeric thresholds; if data is missing or ambiguous, yields `PENDING_HUMAN_REVIEW` rather than guessing.

---

## 6. Requirement-to-Evidence Mapping

The engine centralizes source and document associations:

| Requirement Focus | Expected `SourceType` | Expected `DocumentType` | Evaluation Basis |
| :--- | :--- | :--- | :--- |
| GST Registration | `SourceType.GST` | `DocumentType.GST_CERTIFICATE` | VerificationStatus = VERIFIED |
| PAN Verification | `SourceType.PAN` | `DocumentType.PAN_CARD` | VerificationStatus = VERIFIED |
| Udyam MSME | `SourceType.UDYAM` | `DocumentType.UDYAM_CERTIFICATE` | VerificationStatus = VERIFIED |
| Debarment / Blacklisting | `SourceType.DEBARMENT_LIST` | `DocumentType.DECLARATION` | No active debarment found |
| OEM Authorization | `SourceType.CUSTOM_API` / `DOCUMENT` | `DocumentType.OEM_AUTHORIZATION` | Document presence & integrity |
| Make in India | `SourceType.DOCUMENT` | `DocumentType.MAKE_IN_INDIA_DOCUMENT` | Document & declaration claim |

---

## 7. Contradiction Handling

When multiple evidence records exist for the same requirement, the engine does **not** arbitrarily pick the first record or ignore discrepancies:
1. If evidence contains multiple conflicting values (e.g., Evidence A has GSTIN `29ABCDE1234F1Z5` verified, but Evidence B has `29ABCDE9999F1Z5` mismatch), the requirement status evaluates to **`CONTRADICTORY`**.
2. Both evidence IDs are captured in `evidenceIds` of `ComplianceEvaluationResultDto`.
3. The reason explicitly details the contradiction:
   `"Conflicting evidence records detected for source GST. Evidence IDs: [...]"`

---

## 8. Overall Status Logic

The overall status is determined through a deterministic priority cascade, never solely by the numerical percentage:

```
IF any mandatory requirement is NON_COMPLIANT:
    overallStatus = NON_COMPLIANT
ELSE IF any mandatory requirement is CONTRADICTORY:
    overallStatus = REVIEW
ELSE IF any mandatory requirement is MISSING:
    overallStatus = REVIEW
ELSE IF any mandatory requirement is UNVERIFIED:
    overallStatus = REVIEW
ELSE IF any mandatory requirement is PENDING_HUMAN_REVIEW:
    overallStatus = REVIEW
ELSE IF all mandatory requirements are COMPLIANT:
    overallStatus = COMPLIANT
ELSE:
    overallStatus = REVIEW
```

### Precedence Table:
| Condition | Overall Status | Action Required |
| :--- | :--- | :--- |
| Any mandatory requirement is `NON_COMPLIANT` | `NON_COMPLIANT` | Clear automated failure detected. |
| Any mandatory requirement is `MISSING`, `UNVERIFIED`, `CONTRADICTORY`, or `PENDING_HUMAN_REVIEW` | `REVIEW` | Requires procurement officer review and manual validation. |
| All mandatory requirements are `COMPLIANT` | `COMPLIANT` | All mandatory automated checks satisfied. |

---

## 9. Compliance Percentage

The compliance percentage is a descriptive progress metric and **not** a qualification decision:

$$\text{compliancePercentage} = \left( \frac{\text{Number of evaluated requirements that are COMPLIANT}}{\text{Number of applicable requirements}} \right) \times 100$$

- Requirements marked `NOT_APPLICABLE` are excluded from the denominator.
- Rounded to one decimal place.
- No arbitrary risk weights are used at this step.

---

## 10. REST APIs

### Tender Requirement Endpoints:

#### 1. Create Tender Requirement
* **`POST /api/tenders/{tenderId}/requirements`**
* **Request Body**:
```json
{
  "requirementCode": "REQ-GST",
  "name": "GST Registration",
  "description": "Valid GSTIN required under CGST Act",
  "requirementType": "REGISTRATION",
  "mandatory": true,
  "expectedSourceType": "GST",
  "expectedDocumentType": "GST_CERTIFICATE"
}
```
* **Response**: `201 Created` with `RequirementResponseDto`.

#### 2. Get Tender Requirements
* **`GET /api/tenders/{tenderId}/requirements`**
* **Response**: `200 OK` with `List<RequirementResponseDto>`.

#### 3. Get Requirement by ID
* **`GET /api/requirements/{requirementId}`**
* **Response**: `200 OK` with `RequirementResponseDto`.

---

### Bid Compliance Endpoints:

#### 4. Evaluate Bid Compliance
* **`POST /api/bids/{bidId}/compliance/evaluate`**
* **Response**: `200 OK` with `BidComplianceResultDto`.

#### 5. Get Stored Bid Compliance
* **`GET /api/bids/{bidId}/compliance`**
* **Response**: `200 OK` with latest `BidComplianceResultDto`.

---

## 11. Example Evaluation Response

```json
{
  "bidId": "8b5f3a02-12a8-4229-873b-fba0e3e7f411",
  "tenderId": "2f4e9a11-c9f2-4bd5-912a-4318c679a9b1",
  "bidderId": "d1c44e90-e51c-4392-b43d-045a2bc1d89e",
  "overallStatus": "REVIEW",
  "compliancePercentage": 66.7,
  "mandatoryRequirementCount": 3,
  "compliantCount": 2,
  "nonCompliantCount": 0,
  "missingCount": 1,
  "unverifiedCount": 0,
  "contradictoryCount": 0,
  "pendingReviewCount": 0,
  "requirementResults": [
    {
      "requirementId": "3a11b2c3-4455-6677-8899-aabbccddeeff",
      "requirementCode": "REQ-GST",
      "requirementName": "Valid GST Registration",
      "mandatory": true,
      "status": "COMPLIANT",
      "confidence": 1.0,
      "evidenceIds": ["e1a2b3c4-0000-1111-2222-333344445555"],
      "supportingClaimIds": ["c1a2b3c4-0000-1111-2222-333344445555"],
      "reason": "Registration verified by external gateway source GST.",
      "evaluatedAt": "2026-09-23T19:30:00"
    },
    {
      "requirementId": "4b22c3d4-5566-7788-9900-bbccddeeff00",
      "requirementCode": "REQ-PAN",
      "requirementName": "Permanent Account Number",
      "mandatory": true,
      "status": "COMPLIANT",
      "confidence": 1.0,
      "evidenceIds": ["e2a2b3c4-0000-1111-2222-333344445555"],
      "supportingClaimIds": ["c2a2b3c4-0000-1111-2222-333344445555"],
      "reason": "Registration verified by external gateway source PAN.",
      "evaluatedAt": "2026-09-23T19:30:00"
    },
    {
      "requirementId": "5c33d4e5-6677-8899-0011-ccddeeff0011",
      "requirementCode": "REQ-OEM",
      "requirementName": "OEM Authorization Certificate",
      "mandatory": true,
      "status": "MISSING",
      "confidence": 1.0,
      "evidenceIds": [],
      "supportingClaimIds": [],
      "reason": "Required document OEM_AUTHORIZATION is missing from the submitted bid.",
      "missingItems": ["Document of type OEM_AUTHORIZATION"],
      "evaluatedAt": "2026-09-23T19:30:00"
    }
  ],
  "summary": "Compliance requires officer review. 1 mandatory requirement(s) are missing or unresolved.",
  "evaluatedAt": "2026-09-23T19:30:00"
}
```

---

## 12. Human Decision Boundary

> **IMPORTANT LEGAL & POLICY NOTICE**  
> The Tender-Specific Compliance Engine produces an **automated compliance assessment** to assist procurement officers. It does **NOT** make the final procurement qualification, disqualification, or award decision. All outputs are advisory findings, highlighting missing documents, contradictory sources, and verification discrepancies for human adjudication.

---

## 13. Limitations

1. **Deterministic Rules Only**: Uses explicit rule strategies. No LLM, probabilistic NLP, or unstructured text interpretation.
2. **Current Domain Scope**: Supports registration verification (GST, PAN, Udyam, Debarment), document presence/integrity, standard declarations, and basic numeric eligibility.
3. **No Risk Scoring**: Step 7 is purely compliance evaluation; risk scoring and cross-bidder collusion detection are deferred to subsequent steps.
