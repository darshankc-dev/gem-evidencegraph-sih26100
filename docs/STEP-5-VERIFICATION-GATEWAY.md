# Step 5: Verification Gateway + Evidence Creation

## Overview & Architecture

The **Verification Gateway** is the core bridge in **GeM EvidenceGraph** that transitions asserted data found in documents (`Claims`) into verifiable truth verified by authoritative sources (`Evidence`).

> **Architectural Principle:**
> $$\mathbf{EXTRACTION \neq VERIFICATION}$$
>
> A document extraction process only produces **Claims** (unverified assertions). The Verification Gateway queries authoritative registries (e.g., GSTN, NSDL PAN, MSME Udyam, Debarment registers) through pluggable adapters to determine whether the claim holds, creating auditable **Evidence** nodes and graph edges (**EvidenceRelationship**).

> [!NOTE]
> **Mock adapters are used only because external government source integration is not implemented in this prototype.** Real API connectors can replace these mock adapters without modifying the gateway or domain logic.

---

## 1. Verification Architecture & Flow

```
DOCUMENT
  ↓
CLAIM (e.g. GSTIN = "29ABCDE1234F1Z5", extractionMethod = DETERMINISTIC)
  ↓
POST /api/claims/{claimId}/verify
  ↓
VERIFICATION GATEWAY (VerificationGatewayService)
  ↓
ADAPTER SELECTION (Registry matching SourceType: GST, PAN, UDYAM, DEBARMENT_LIST)
  ↓
SOURCE ADAPTER (Executes verification, captures raw snapshot, calculates latency/confidence)
  ↓
VERIFICATION RESULT (VerificationResultDto: expectedValue vs observedValue)
  ↓
PROVENANCE HASHING (Deterministic SHA-256 via ChecksumUtil)
  ↓
IDEMPOTENCY CHECK (Reuses existing identical verification records if duplicate)
  ↓
EVIDENCE (Persisted with VerificationStatus, sourceSystem, adapterVersion, provenanceHash)
  ↓
EVIDENCE RELATIONSHIP (CLAIM --VERIFIED_BY / CONTRADICTS--> EVIDENCE)
```

---

## 2. Pluggable Adapter Pattern

The gateway maintains a registry of `VerificationAdapter` components:

```java
public interface VerificationAdapter {
    SourceType getSourceType();
    String getAdapterVersion();
    boolean supports(Claim claim);
    VerificationResultDto verify(Claim claim);
}
```

The adapter:
- Validates whether it supports the claim's field name (e.g. GST adapter checks for `GSTIN`, `LEGAL_NAME`, `ADDRESS`).
- Queries the authoritative registry or mock fixture.
- Returns a structured `VerificationResultDto` containing `expectedValue`, `observedValue`, `verificationStatus`, `confidence`, and `rawResponseSnapshot`.
- **Crucial Separation:** The adapter **never** directly creates `Evidence` entities; only the `VerificationGateway` manages persistence, provenance hashing, idempotency, and graph edge generation.

---

## 3. Mock Source Strategy

Four replaceable mock adapters are provided:
1. `MockGstVerificationAdapter` (Source: `GST`) — Emulates GSTN taxpayer registry queries.
2. `MockPanVerificationAdapter` (Source: `PAN`) — Emulates Income Tax Department / NSDL PAN validation.
3. `MockUdyamVerificationAdapter` (Source: `UDYAM`) — Emulates Ministry of MSME Udyam registration lookups.
4. `MockDebarmentVerificationAdapter` (Source: `DEBARMENT_LIST`) — Emulates CPPP and GeM procurement blacklist and debarment registers.

Each adapter supports:
- Programmatic test registrations (`registerMockResponse(expected, result)`).
- Deterministic keyword behavior for testing without a database (`"MISMATCH"`, `"UNAVAILABLE"`, `"UNVERIFIED"`, `"DEBARRED"`).
- Default deterministic verification for well-formatted procurement identifiers.
- Fault simulation (`setSimulateUnavailable(true)`).

---

## 4. VerificationStatus Semantics & Safety Rules

| Status | Meaning | Graph Edge Created |
|---|---|---|
| `VERIFIED` | Source evidence matches the claim. | `CLAIM --VERIFIED_BY--> EVIDENCE` |
| `MISMATCH` | Source evidence exists but conflicts with the claim. | `CLAIM --CONTRADICTS--> EVIDENCE` |
| `UNVERIFIED` | Source does not provide enough information to verify the claim. | *None* |
| `SOURCE_UNAVAILABLE` | External source could not be reached, timed out, or threw an error. | *None* |
| `NOT_APPLICABLE` | The source does not apply to this claim. | *None* |
| `PENDING_HUMAN_REVIEW` | The result requires manual human verification. | *None* |

### Non-Negotiable Safety Rule
$$\mathbf{SOURCE\_UNAVAILABLE \neq VERIFIED}$$
$$\mathbf{UNVERIFIED \neq VERIFIED}$$
$$\mathbf{PENDING\_HUMAN\_REVIEW \neq VERIFIED}$$

Under no circumstances is an unverified or unavailable source record treated as verified evidence.

---

## 5. Domain Model & Provenance

### Claim → Evidence Relationship
* `Evidence` links directly to its parent `Claim`:
  ```java
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "claim_id")
  private Claim claim;
  ```
* Provenance fields:
  - `sourceSystem`: Identifier of the source registry (e.g. `GSTN-PORTAL-MOCK`, `ITD-NSDL-MOCK`).
  - `adapterVersion`: Adapter build/contract version (e.g. `mock-gst-v1.0`).
  - `rawResponseSnapshot`: Raw JSON payload returned by the source adapter for forensic auditability.
  - `provenanceHash`: Cryptographic SHA-256 hash computed deterministically over `sourceType:sourceReference:observedValue:rawResponseSnapshot`.

### Evidence Relationships
`EvidenceRelationship` supports both Claim-to-Evidence and Evidence-to-Evidence edges:
* `sourceClaim`: `Claim` being verified or contradicted.
* `sourceEvidence`: Optional source `Evidence` for evidence-to-evidence graph links (`SUPPORTS`, `RELATED_TO`).
* `targetEvidence`: Resulting `Evidence` node.
* `relationshipType`: `VERIFIED_BY`, `CONTRADICTS`, `SUPPORTS`, `RELATED_TO`.

---

## 6. REST API Endpoints

### 1. Verify Individual Claim
* **Endpoint:** `POST /api/claims/{claimId}/verify`
* **Request:**
  ```json
  {
    "bidId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "requestedSourceType": "GST"
  }
  ```
* **Response (`200 OK`):**
  ```json
  {
    "claimId": "e4eaaaf2-d142-11e1-b3e4-080027620cdd",
    "sourceType": "GST",
    "verificationStatus": "VERIFIED",
    "expectedValue": "29ABCDE1234F1Z5",
    "observedValue": "29ABCDE1234F1Z5",
    "evidenceId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "relationshipId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "confidence": 1.0,
    "reason": "Claim value matches source evidence."
  }
  ```

### 2. Bulk Verify Bid Claims
* **Endpoint:** `POST /api/bids/{bidId}/verify`
* **Query Parameter:** `sourceType` (optional, defaults to inferring best adapter per claim)
* **Response (`200 OK`):**
  ```json
  {
    "bidId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "totalClaimsScanned": 3,
    "verifiedCount": 3,
    "mismatchCount": 0,
    "unverifiedCount": 0,
    "unavailableCount": 0,
    "results": [...]
  }
  ```

---

## 7. Future Transition to Real Government APIs

To transition to production:
1. Implement `VerificationAdapter` interfaces backed by real HTTPS REST/SOAP clients (e.g. GSTN Sandbox/Production APIs, Protean/NSDL PAN APIs, MSME Udyam APIs).
2. Configure OAuth2 credentials or mTLS certificates inside secure configuration properties or secret managers.
3. No changes to `VerificationGatewayService`, `Claim`, `Evidence`, or `ClaimVerificationController` are required.
