# Step 6 — Deterministic Entity Resolution Engine

> **Notice**: This implementation is deterministic and does not use machine learning, embeddings, or LLM-based entity resolution.

---

## 1. Purpose

The **Deterministic Entity Resolution Engine** resolves whether business identity representations gathered across disparate procurement sources refer to the exact same underlying legal or commercial entity.

In public procurement on Government e-Marketplace (GeM), bidder identity information appears across diverse artifacts:
* Bidder profile forms submitted at bid registration
* Bid proposal documents (PDF, scans, forms)
* Regulatory registry evidence (Income Tax Department / NSDL PAN records)
* Goods and Services Tax Network (GSTN) records
* MSME Udyam registration certificates
* Ministry of Corporate Affairs (MCA) records

This engine normalizes these variations and evaluates identity consistency deterministically, producing an explainable audit trail without probabilistic opacity.

---

## 2. Why Entity Resolution is Required

In procurement integrity evaluation:
1. **Syntactic and Formatting Discrepancies**: The same business legally named `ABC TECHNOLOGIES PRIVATE LIMITED` is frequently entered as `ABC Technologies Pvt. Ltd.` on a document, `ABC Technologies` in an MSME certificate, or `ABC TECHNOLOGIES PVT LTD` in GST portal records. Without normalization, simple string comparisons cause false mismatches.
2. **Fraud Prevention & Collusion Detection**: Shell entities or colluding bidders frequently submit slight name or address permutations while sharing statutory tax IDs (PAN/GSTIN), or conversely use legitimate business names with substituted tax IDs.
3. **Auditability & Legal Explainability**: Procurement rejection or compliance clearance cannot rely on "black-box" fuzzy match models. Procurement officers require exact deterministic justifications specifying which statutory identifiers matched or contradicted.

---

## 3. Normalization Rules

All attributes undergo strict, reproducible canonicalization before comparison via `EntityNormalizationService`:

### A. Legal Name
* **Uppercase conversion** and removal of excessive whitespace.
* **Punctuation removal**: Strips periods, commas, quotation marks, and hyphens when safe.
* **Canonical Legal Suffix Mapping**:
  * `PVT LTD`, `PVT. LTD.`, `PRIVATE LTD`, `PVT LIMITED`, `PRIVATE LIMITED` $\to$ `PRIVATE LIMITED`
  * `LTD`, `LTD.`, `LIMITED` $\to$ `LIMITED`
  * `LLP`, `L.L.P.`, `LIMITED LIABILITY PARTNERSHIP` $\to$ `LLP`
  * `PROP`, `PROPRIETORSHIP`, `PROPRIETARY` $\to$ `PROPRIETORSHIP`
  * `INC`, `INC.`, `INCORPORATED` $\to$ `INCORPORATED`
  * `CORP`, `CORP.`, `CORPORATION` $\to$ `CORPORATION`
  * `CO`, `CO.`, `COMPANY` $\to$ `COMPANY`
* **Preservation of Distinct Entities**: Meaningful distinguishing tokens are preserved. For example, `ABC Technologies` and `ABC Technology Solutions` remain distinct and do not match without independent corroborating identifiers.

### B. Registered Address
* **Whitespace and Case**: Converted to uppercase; tabs, carriage returns, and newlines collapsed to single spaces.
* **Punctuation**: Commas, semicolons, colons, slashes, hash symbols (`#`), and periods removed.
* **Standardized Common Abbreviations**:
  * `RD` $\to$ `ROAD`
  * `ST` $\to$ `STREET`
  * `NO` / `NO.` $\to$ `NUMBER`
  * `FLR` $\to$ `FLOOR`
  * `BLDG` $\to$ `BUILDING`
  * `APT` $\to$ `APARTMENT`
  * `SECT` $\to$ `SECTOR`
  * `DIST` $\to$ `DISTRICT`
* *Geocoding and external maps are strictly avoided.*

### C. PAN (Permanent Account Number)
* Uppercase alphanumeric cleanup.
* Removal of spaces, hyphens, and non-alphanumeric characters.
* Preserves 10-character structure (`[A-Z]{5}[0-9]{4}[A-Z]`).

### D. GSTIN (GST Identification Number)
* Uppercase conversion.
* Removal of whitespace and hyphens.
* Preserves 15-character alphanumeric tax identifier.

### E. MSME Udyam Number
* Uppercase conversion.
* Space-to-hyphen conversion and collapsing of repeated hyphens.
* Format: `UDYAM-XX-00-0000000`.

### F. Email & Phone
* **Email**: Lowercased and trimmed.
* **Phone**: Stripped of formatting characters (`()`, `-`, `.` `,` spaces); standardizes Indian prefixes (`+91` or `91` followed by 10 digits $\to$ 10 digits).

---

## 4. Strong vs Supporting Identifiers

Identifiers are tiered into two distinct tiers of identity strength:

| Identifier | Tier | Identity Strength | Comparison Rule |
| :--- | :--- | :--- | :--- |
| **PAN** | Strong | High (Statutory Identity) | Exact normalized match required. Mismatch is a hard contradiction. |
| **GSTIN** | Strong | High (Tax Registration) | Exact normalized match required. Cross-checks embedded PAN (chars 3-12). |
| **Udyam Number** | Strong | High (Enterprise Registration) | Exact normalized match required. |
| **Legal Name** | Supporting | Medium | Exact normalized match or token equivalence (order variations, high token overlap). |
| **Registered Address** | Supporting | Medium | Exact normalized match or token Jaccard consistency ($\ge 0.65$). |
| **Email** | Supporting | Low / Supplementary | Secondary contact; difference does not disqualify identity. |
| **Phone** | Supporting | Low / Supplementary | Secondary contact; businesses may have multiple contact numbers. |

> **Dominance Principle**: A match on a strong statutory identifier (such as PAN or GSTIN) dominates minor variations in textual supporting attributes (such as address punctuation or legal name suffix formatting).

---

## 5. Match Statuses

The engine outputs one of four deterministic statuses (`EntityMatchStatus`):

1. **`MATCH`**:
   * One or more strong statutory identifiers (PAN, GSTIN, Udyam) match with no strong contradiction.
   * Supporting attributes (legal name, address) are consistent or matching.
   * Confidence: `1.0`.

2. **`PROBABLE_MATCH`**:
   * No statutory contradictions exist.
   * Identity attributes are broadly consistent, but statutory identifiers (PAN/GSTIN) are unavailable or missing on one or both sides (e.g., Legal Name + Address match; or Legal Name alone matches).
   * Confidence: `0.65` – `0.80`.

3. **`MISMATCH`**:
   * A hard contradiction is detected in a strong statutory identifier (PAN mismatch, GSTIN mismatch, Udyam mismatch, or embedded PAN contradiction).
   * OR multiple independent identity attributes (Legal Name AND Registered Address) strongly conflict with no bridging identifier.
   * Confidence: `0.0`.

4. **`INSUFFICIENT_DATA`**:
   * Inadequate identity information exists on one or both sides to perform a reliable comparison (e.g., both profiles empty, or only phone/email present without name, address, or statutory IDs).
   * Confidence: `0.0`.

---

## 6. Contradiction Handling

The engine treats identity conflicts with strict precedence:
* **Statutory Identifier Contradiction**: If both profiles provide a PAN, GSTIN, or Udyam number and the values differ, the result is unconditionally **`MISMATCH`**.
* **Embedded Identifier Contradiction**: An Indian GSTIN embeds the 10-character PAN in characters 3 through 12. If a profile presents PAN `ABCDE1234F` while the opposing profile presents GSTIN `29XYZAB9876K1Z5`, the embedded PAN conflict triggers a hard `MISMATCH`.
* **Contact Differences**: Businesses routinely maintain multiple email addresses and phone numbers across regional offices or departments. A difference in phone or email is recorded in `mismatchedAttributes`, but **never** triggers an overall `MISMATCH` if name and identifiers match.

---

## 7. Missing-Data Handling

A foundational design requirement of the engine is that **missing information is never conflated with contradictory information**:

* If an attribute is present in Profile A but missing or null in Profile B:
  * The attribute is recorded in `missingAttributes`.
  * It is **not** added to `mismatchedAttributes`.
  * It does not trigger a `MISMATCH`.
* **Example**:
  * Bidder PAN: `ABCDE1234F`
  * GST Evidence: PAN field omitted / unavailable
  * Outcome: PAN is categorized under `missingAttributes`. The engine evaluates remaining attributes (GSTIN, Legal Name, Address).

---

## 8. REST API Examples

### A. Arbitrary Profile Comparison Endpoint
**`POST /api/entity-resolution/compare`**

#### Request:
```json
{
  "left": {
    "legalName": "ABC Technologies Pvt. Ltd.",
    "pan": "ABCDE1234F",
    "gstin": "29ABCDE1234F1Z5",
    "udyamNumber": "UDYAM-KA-01-0000001",
    "registeredAddress": "12 MG Road Mysuru Karnataka"
  },
  "right": {
    "legalName": "ABC TECHNOLOGIES PRIVATE LIMITED",
    "pan": "ABCDE1234F",
    "gstin": "29ABCDE1234F1Z5",
    "udyamNumber": "UDYAM-KA-01-0000001",
    "registeredAddress": "12, MG ROAD, MYSURU, KARNATAKA"
  }
}
```

#### Response:
```json
{
  "matchStatus": "MATCH",
  "confidence": 1.0,
  "comparedAttributes": [
    "PAN",
    "GSTIN",
    "UDYAM_NUMBER",
    "LEGAL_NAME",
    "REGISTERED_ADDRESS"
  ],
  "matchedAttributes": [
    "PAN",
    "GSTIN",
    "UDYAM_NUMBER",
    "LEGAL_NAME",
    "REGISTERED_ADDRESS"
  ],
  "mismatchedAttributes": [],
  "missingAttributes": [],
  "explanation": "MATCH: Statutory identifier(s) match and normalized legal name is consistent. PAN matches exactly ('ABCDE1234F'). GSTIN matches exactly ('29ABCDE1234F1Z5'). UDYAM_NUMBER matches exactly ('UDYAM-KA-01-0000001'). Normalized legal name matches ('ABC TECHNOLOGIES PRIVATE LIMITED'). Normalized address matches ('12 MG ROAD MYSURU KARNATAKA')."
}
```

### B. Bidder vs Verified Evidence Endpoint
**`POST /api/entity-resolution/bidders/{bidderId}/resolve`**

#### Response:
```json
{
  "matchStatus": "MATCH",
  "confidence": 1.0,
  "comparedAttributes": ["PAN", "GSTIN", "LEGAL_NAME"],
  "matchedAttributes": ["PAN", "GSTIN", "LEGAL_NAME"],
  "mismatchedAttributes": [],
  "missingAttributes": ["UDYAM_NUMBER", "REGISTERED_ADDRESS", "EMAIL", "PHONE"],
  "explanation": "MATCH: Statutory identifier(s) match and normalized legal name is consistent. PAN matches exactly ('ABCDE1234F'). GSTIN matches exactly ('29ABCDE1234F1Z5'). Normalized legal name matches ('ABC TECHNOLOGIES PRIVATE LIMITED')."
}
```

---

## 9. Test Scenarios and Coverage

The test suite contains 30 dedicated unit tests across normalization and resolution, including all 17 mandatory test cases:

1. **Exact identity**: All identifiers match $\to$ `MATCH` (confidence 1.0).
2. **Formatting differences**: `Pvt. Ltd.` vs `Private Limited` $\to$ `MATCH`.
3. **Address punctuation differences**: `12, MG Road, Mysuru` vs `12 MG ROAD MYSURU` $\to$ `MATCH`.
4. **Strong contradiction (PAN)**: `ABCDE1234F` vs `XYZAB9876K` $\to$ `MISMATCH`.
5. **Strong contradiction (GSTIN)**: `29ABCDE1234F1Z5` vs `27XYZAB9876K1Z9` $\to$ `MISMATCH`.
6. **Strong contradiction (Udyam)**: Differing Udyam numbers $\to$ `MISMATCH`.
7. **Missing information (PAN)**: PAN missing on one side $\to$ recorded in `missingAttributes`, does not force `MISMATCH`.
8. **Missing information (GSTIN)**: GSTIN missing on one side $\to$ evaluated as `MATCH` via PAN.
9. **Only legal name matching**: Available name matches without statutory IDs $\to$ `PROBABLE_MATCH`.
10. **Empty profiles**: No core attributes $\to$ `INSUFFICIENT_DATA`.
11. **Supporting attributes**: Name + Address match $\to$ `PROBABLE_MATCH`.
12. **Supporting attributes**: Name matches but phone differs $\to$ does not classify as `MISMATCH`.
13. **Bidder vs Verified Evidence (Matching)**: Bidder PAN matches registry evidence $\to$ `MATCH`.
14. **Bidder vs Verified Evidence (Conflicting)**: Conflicting PAN evidence $\to$ `MISMATCH`.
15. **Deterministic Explanation**: Explanation text present on all evaluations.
16. **Safety**: Missing evidence is never treated as a contradiction.
17. **Safety**: `SOURCE_UNAVAILABLE` evidence is never treated as verified identity evidence.
18. **Cross-Check**: Embedded PAN in GSTIN contradicting isolated PAN $\to$ `MISMATCH`.

---

## 10. Limitations

1. **Deterministic Rule Constraints**: Does not handle phonetic spelling errors or extreme typos in legal names (e.g. `Infortech` vs `Infotech`) without supporting statutory identifier matches.
2. **Address Variations**: Variations involving non-standard regional terminology (e.g., `Tehsil` vs `Taluk`) are compared using token overlap; without coordinates or geocoding, completely disparate address phrasings cannot be bridged unless statutory identifiers match.
3. **Registry Scope**: Relies on external verification adapters implemented in Step 5 (PAN, GSTIN, Udyam) to populate verified evidence.

---

## 11. Future Enhancement Possibilities

1. **MCA CIN Integration**: Incorporating Ministry of Corporate Affairs Corporate Identification Numbers (CIN) as a tier-1 strong statutory identifier.
2. **Director / Key Management Personnel (KMP) Cross-Matching**: Comparing DIN (Director Identification Numbers) across corporate filings.
3. **Temporal Historical Tracking**: Recording corporate name changes or amalgamations using historical registrar logs.
4. **Graph Cluster Aggregation**: Linking multi-bidder ownership clusters in Step 7+ for cross-bidder collusion detection.
