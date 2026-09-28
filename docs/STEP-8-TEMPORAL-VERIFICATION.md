# Step 8 — Temporal Verification Engine

## 1. Purpose

The **Temporal Verification Engine** is a deterministic, rule-based evaluation engine for the GeM EvidenceGraph (SIH26100) platform. In public procurement, compliance cannot rely purely on static claims without verifying whether certificates, authorizations, registrations, and credentials were valid at the time the bid was submitted or relative to the tender timeline.

The engine evaluates:
> **"Was this evidence valid at the bid submission date, or was it expired, not yet valid, issued after the bid, issued after tender publication, affected by conflicting dates, or missing required temporal data?"**

---

## 2. Architectural Principles & Invariants

1. **Evaluation Layer Only**: The engine produces factual, explainable temporal findings. It does **not** automatically disqualify bidders or make final procurement decisions.
2. **Deterministic & Rule-Based**: Evaluated strictly through deterministic rules and predefined precedence. No ML, probabilistic scoring, or fuzzy heuristics.
3. **No Date Invention**: The bid submission date is the primary reference date. If absent, the engine reports `MISSING_TEMPORAL_DATA` and never substitutes the current date or today's timestamp.
4. **Idempotent Audit Persistence**: Evaluations are persisted in `temporal_evaluations` preserving historical integrity and idempotency.

---

## 3. Temporal Statuses (`TemporalStatus`)

| Status | Definition | Meaning / Boundary Condition |
| :--- | :--- | :--- |
| **`VALID_AT_BID_DATE`** | Validity window covers bid date. | `validFrom <= bidSubmissionDate <= validUntil` (inclusive boundaries). |
| **`EXPIRED_AT_BID_DATE`** | Evidence expired before bid date. | `validUntil < bidSubmissionDate`. |
| **`NOT_YET_VALID_AT_BID_DATE`** | Validity begins after bid date. | `validFrom > bidSubmissionDate`. |
| **`ISSUED_AFTER_BID`** | Issued/registered after bid date. | `issueDate > bidSubmissionDate`. |
| **`ISSUED_AFTER_TENDER_PUBLICATION`**| Issued after tender publication. | `issueDate > tender.publicationDate` (e.g. OEM authorization). |
| **`CONFLICTING_DATES`** | Contradictory temporal values. | `validFrom > validUntil`, `issueDate > validUntil`, or contradictory timestamps. |
| **`MISSING_TEMPORAL_DATA`** | Expected temporal data absent. | Bid date missing or no dates available for time-bound evidence. |
| **`NOT_APPLICABLE`** | Evidence requires no temporal check. | Static attributes (e.g., PAN name, non-expiring identities). |

---

## 4. Rule Precedence Hierarchy

When multiple temporal rules match or problems exist, the primary status is determined deterministically by priority:

$$\begin{aligned}
1. &\quad \text{CONFLICTING\_DATES} \\
2. &\quad \text{MISSING\_TEMPORAL\_DATA} \\
3. &\quad \text{NOT\_YET\_VALID\_AT\_BID\_DATE} \\
4. &\quad \text{EXPIRED\_AT\_BID\_DATE} \\
5. &\quad \text{ISSUED\_AFTER\_BID} \\
6. &\quad \text{ISSUED\_AFTER\_TENDER\_PUBLICATION} \\
7. &\quad \text{VALID\_AT\_BID\_DATE} \\
8. &\quad \text{NOT\_APPLICABLE}
\end{aligned}$$

All evaluated findings are preserved in `TemporalEvaluationOutcome.allFindings` for complete auditability.

---

## 5. Temporal Rules Implemented

| Rule | Class | Priority | Description |
| :--- | :--- | :---: | :--- |
| **Date Consistency** | `DateConsistencyRule` | 1 | Detects `validFrom > validUntil`, `issueDate > validUntil`, or conflict flags. |
| **Missing Data** | `MissingTemporalDataRule` | 2 | Flags missing bid submission date or missing temporal dates on required evidence. |
| **Future Validity** | `FutureValidityRule` | 3 | Flags `validFrom > bidSubmissionDate`. |
| **Expiration** | `ExpirationRule` | 4 | Flags `validUntil < bidSubmissionDate`. |
| **Issue After Bid** | `IssueAfterBidRule` | 5 | Flags `issueDate > bidSubmissionDate`. |
| **Issue After Pub** | `IssueAfterTenderPublicationRule` | 6 | Flags `issueDate > tender.publicationDate`. |
| **Validity Window** | `ValidityWindowRule` | 7 | Confirms `validFrom <= bidSubmissionDate <= validUntil` (inclusive). |
| **Not Applicable** | `NotApplicableRule` | 8 | Identifies static non-temporal evidence. |

---

## 6. Database Schema (`temporal_evaluations`)

```sql
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
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
```

Indexes:
* `idx_temp_eval_bid` ON `temporal_evaluations(bid_id)`
* `idx_temp_eval_tender` ON `temporal_evaluations(tender_id)`
* `idx_temp_eval_evidence` ON `temporal_evaluations(evidence_id)`
* `idx_temp_eval_status` ON `temporal_evaluations(status)`

---

## 7. REST Endpoints

### 1. Evaluate Bid Temporal
* **`POST /api/bids/{bidId}/temporal/evaluate`**
* Evaluates all applicable evidence for a bid, persists idempotent evaluation records, and returns full itemized results.

### 2. Get Stored Bid Temporal
* **`GET /api/bids/{bidId}/temporal`**
* Retrieves latest stored evaluations for the bid. If none exist, runs evaluation on-demand.

### 3. Get Evidence Temporal History
* **`GET /api/evidence/{evidenceId}/temporal`**
* Retrieves evaluation history for a specific evidence item.

---

## 8. Sample JSON Response

```json
{
  "bidId": "8b5f3a02-12a8-4229-873b-fba0e3e7f411",
  "tenderId": "2f4e9a11-c9f2-4bd5-912a-4318c679a9b1",
  "evaluatedAt": "2026-09-24T10:00:00",
  "totalEvaluated": 3,
  "validCount": 1,
  "expiredCount": 1,
  "futureValidityCount": 0,
  "issuedAfterBidCount": 0,
  "issuedAfterTenderPublicationCount": 1,
  "conflictingCount": 0,
  "missingTemporalDataCount": 0,
  "notApplicableCount": 0,
  "results": [
    {
      "evaluationId": "c4b82d01-e231-4890-a7d2-32a76f2b4890",
      "bidId": "8b5f3a02-12a8-4229-873b-fba0e3e7f411",
      "tenderId": "2f4e9a11-c9f2-4bd5-912a-4318c679a9b1",
      "evidenceId": "e1a2b3c4-0000-1111-2222-333344445555",
      "referenceDate": "2026-09-20T10:00:00",
      "validFrom": "2026-01-01T00:00:00",
      "validUntil": "2026-12-31T00:00:00",
      "status": "VALID_AT_BID_DATE",
      "reason": "Evidence validity window covers the bid submission date.",
      "evaluatedAt": "2026-09-24T10:00:00"
    },
    {
      "evaluationId": "d5c93e12-f342-5901-b8e3-43b87a3c5901",
      "bidId": "8b5f3a02-12a8-4229-873b-fba0e3e7f411",
      "tenderId": "2f4e9a11-c9f2-4bd5-912a-4318c679a9b1",
      "evidenceId": "e2b3c4d5-0000-1111-2222-333344445555",
      "referenceDate": "2026-09-20T10:00:00",
      "validUntil": "2026-08-31T00:00:00",
      "status": "EXPIRED_AT_BID_DATE",
      "reason": "Evidence expired before the bid submission date.",
      "evaluatedAt": "2026-09-24T10:00:00"
    }
  ]
}
```
